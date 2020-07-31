package org.gwtproject.rpc.serial.processor;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.gwtproject.rpc.gwtapt.JTypeUtils;
import org.gwtproject.rpc.serial.processor.TypePaths.TypePath;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds a {@link SerializableTypeOracle} for a given set of root types.
 *
 * <p>
 * There are two goals for this builder. First, discover the set of serializable
 * types that can be serialized if you serialize one of the root types. Second,
 * to make sure that all root types can actually be serialized by GWT.
 * </p>
 *
 * <p>
 * To find the serializable types, it includes the root types, and then it
 * iteratively traverses the type hierarchy and the fields of any type already
 * deemed serializable. To improve the accuracy of the traversal there is a
 * computations of the exposure of type parameters. When the traversal reaches a
 * parameterized type, these exposure values are used to determine how to treat
 * the arguments.
 * </p>
 *
 * <p>
 * A type qualifies for serialization if it or one of its subtypes is
 * automatically or manually serializable. Automatic serialization is selected
 * if the type is assignable to @link IsSerializable or {@link Serializable}
 * or if the type is a primitive type such as int, boolean, etc. Manual
 * serialization is selected if there exists another type with the same fully
 * qualified name concatenated with "_CustomFieldSerializer". If a type
 * qualifies for both manual and automatic serialization, manual serialization
 * is preferred.
 * </p>
 *
 * <p>
 * Some types may be marked as "enhanced," either automatically by the presence
 * of a JDO <code>@PersistenceCapable</code> or JPA <code>@Entity</code> tag on
 * the class definition, or manually by extending the 'rpc.enhancedClasses'
 * configuration property in the GWT module XML file. For example, to manually
 * mark the class com.google.myapp.MyPersistentClass as enhanced, use:
 *
 * <pre>
 * &lt;extend-configuration-property name='rpc.enhancedClasses'
 *     value='com.google.myapp.MyPersistentClass'/&gt;
 * </pre>
 *
 * <p>
 * Enhanced classes are checked for the presence of additional serializable
 * fields on the server that were not defined in client code as seen by the GWT
 * compiler. If it is possible for an instance of such a class to be transmitted
 * bidrectionally between server and client, a special RPC rule is used. The
 * server-only fields are serialized using standard Java serialization and sent
 * between the client and server as a blob of opaque base-64 encoded binary
 * data. When an instance is sent from client to server, the server instance is
 * populated by invoking setter methods where possible rather than by setting
 * fields directly. This allows APIs such as JDO the opportunity to update the
 * object state properly to take into account changes that may have occurred to
 * the object's state while resident on the client.
 * </p>
 */
public class SerializableTypeOracleBuilder {

    static class TypeInfoComputed {

        /**
         * <code>true</code> if the type is automatically or manually serializable
         * and the corresponding checks succeed.
         */
        private boolean fieldSerializable = false;

        /**
         * <code>true</code> if this type might be instantiated.
         */
        private boolean instantiable = false;

        /**
         * <code>true</code> if there are instantiable subtypes assignable to this
         * one.
         */
        private boolean instantiableSubtypes;

        /**
         * All instantiable types found when this type was queried, including the
         * type itself. (Null until calculated.)
         */
        private Set<TypeMirror> instantiableTypes;

        /**
         * Custom field serializer or <code>null</code> if there isn't one.
         */
        private final TypeElement manualSerializer;

//        /**
//         * <code>true</code> if this class might be enhanced on the server to
//         * contain extra fields.
//         */
//        private final boolean maybeEnhanced;

        /**
         * Path used to discover this type.
         */
        private final TypePath path;

        /**
         * The state that this type is currently in.
         */
        private TypeState state = TypeState.NOT_CHECKED;

        /**
         * {@link TypeMirror} associated with this metadata.
         */
        private final TypeMirror type;

        private TypeInfoComputed(TypeMirror type, TypePath path, SerializingTypes typeOracle) {
            this.type = type;
            this.path = path;
            if (type.getKind() == TypeKind.DECLARED) {
                manualSerializer = findCustomFieldSerializer(typeOracle, type);
//                maybeEnhanced = hasJdoAnnotation(typeClass) || hasJpaAnnotation(typeClass);
            } else {
                manualSerializer = null;
//                maybeEnhanced = false;
            }
        }

        public TypePath getPath() {
            return path;
        }

        public TypeMirror getType() {
            return type;
        }

        public boolean hasInstantiableSubtypes() {
            return instantiable || instantiableSubtypes || state == TypeState.CHECK_IN_PROGRESS;
        }

        public boolean isDone() {
            return state == TypeState.CHECK_DONE;
        }

        public boolean isFieldSerializable() {
            return fieldSerializable;
        }

        public boolean isInstantiable() {
            return instantiable;
        }

        public boolean isManuallySerializable() {
            return manualSerializer != null;
        }

        public boolean isPendingInstantiable() {
            return state == TypeState.CHECK_IN_PROGRESS;
        }

//        public boolean maybeEnhanced() {
//            return maybeEnhanced;
//        }

        public void setFieldSerializable() {
            fieldSerializable = true;
        }

        public void setInstantiable(boolean instantiable) {
            this.instantiable = instantiable;
            if (instantiable) {
                fieldSerializable = true;
            }
            state = TypeState.CHECK_DONE;
        }

        public void setInstantiableSubtypes(boolean instantiableSubtypes) {
            this.instantiableSubtypes = instantiableSubtypes;
        }

        public void setPendingInstantiable() {
            state = TypeState.CHECK_IN_PROGRESS;
        }
    }

    private enum TypeState {
        /**
         * The instantiability of a type has been determined.
         */
        CHECK_DONE("Check succeeded"),
        /**
         * The instantiability of a type is being checked.
         */
        CHECK_IN_PROGRESS("Check in progress"),
        /**
         * The instantiability of a type has not been checked.
         */
        NOT_CHECKED("Not checked");

        private final String message;

        TypeState(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    static final Comparator<TypeMirror> JTYPE_COMPARATOR = new TypeMirrorNameComparator();

    /**
     * No type filtering by default..
     */
    private static final TypeFilter DEFAULT_TYPE_FILTER = new TypeFilter() {
        @Override
        public String getName() {
            return "Default";
        }

        @Override
        public boolean isAllowed(TypeMirror type) {
            return true;
        }
    };

//    /**
//     * A reference to the annotation class
//     * javax.jdo.annotations.PersistenceCapable used by the JDO API. May be null
//     * if JDO is not present in the runtime environment.
//     */
//    private static Class<? extends Annotation> JDO_PERSISTENCE_CAPABLE_ANNOTATION = null;
//
//    /**
//     * A reference to the method 'String
//     * javax.jdo.annotations.PersistenceCapable.detachable()'.
//     */
//    private static Method JDO_PERSISTENCE_CAPABLE_DETACHABLE_METHOD;
//
//    /**
//     * A reference to the annotation class javax.persistence.Entity used by the
//     * JPA API. May be null if JPA is not present in the runtime environment.
//     */
//    private static Class<? extends Annotation> JPA_ENTITY_ANNOTATION = null;

//    static {
//        try {
//            JDO_PERSISTENCE_CAPABLE_ANNOTATION =
//                    Class.forName("javax.jdo.annotations.PersistenceCapable").asSubclass(Annotation.class);
//            JDO_PERSISTENCE_CAPABLE_DETACHABLE_METHOD =
//                    JDO_PERSISTENCE_CAPABLE_ANNOTATION.getDeclaredMethod("detachable", (Class[]) null);
//        } catch (ClassNotFoundException e) {
//            // Ignore, JDO_PERSISTENCE_CAPABLE_ANNOTATION will be null
//        } catch (NoSuchMethodException e) {
//            JDO_PERSISTENCE_CAPABLE_ANNOTATION = null;
//        }
//
//        try {
//            JPA_ENTITY_ANNOTATION =
//                    Class.forName("javax.persistence.Entity").asSubclass(Annotation.class);
//        } catch (ClassNotFoundException e) {
//            // Ignore, JPA_ENTITY_CAPABLE_ANNOTATION will be null
//        }
//    }

    static boolean canBeInstantiated(SerializingTypes types, TypeMirror type, ProblemReport problems) {
        TypeElement typeElt = (TypeElement) types.getTypes().asElement(type);
        if (typeElt.getKind() != ElementKind.ENUM) {
            if (typeElt.getModifiers().contains(Modifier.ABSTRACT)) {
                // Abstract types will be picked up if there is an instantiable
                // subtype.
                return false;
            }

            boolean isJreType = isInStandardJavaPackage(ClassName.get(typeElt).toString());

            boolean isDefaultInstantiable = ElementFilter.constructorsIn(typeElt.getEnclosedElements())
                    .stream().anyMatch(ctor -> {
                        if (!ctor.getParameters().isEmpty()) {
                            return false;
                        }
                        // JRE classes don't get their serializer built into their own package,
                        // so the default constructor must be public
                        if (isJreType && !ctor.getModifiers().contains(Modifier.PUBLIC)) {
                            return false;
                        }
                        // For non-JRE classes we can generate serializers in their package,
                        // and can allow constructors to be non-private
                        if (ctor.getModifiers().contains(Modifier.PRIVATE)) {
                            return false;
                        }
                        return true;
                    });
            if (!isDefaultInstantiable && !isManuallySerializable(types, type)) {
                // Warn and return false.
                problems.add(type, ClassName.get(type)
                                + " is not default instantiable (it must have a zero-argument "
                                + "constructor or no constructors at all) and has no custom " + "serializer.",
                        ProblemReport.Priority.DEFAULT);
                return false;
            }
        } else {
      /*
       * Enums are always instantiable regardless of abstract or default
       * instantiability.
       */
        }

        return true;
    }

    /**
     * Finds the custom field serializer for a given type.
     *
     * @param typeOracle
     * @param type
     * @return the custom field serializer for a type or <code>null</code> if
     *         there is not one
     */
    public static TypeElement findCustomFieldSerializer(SerializingTypes typeOracle, TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }

        String customFieldSerializerName = getCustomFieldSerializerName(typeOracle.getTypes().erasure(type));
        return findCustomFieldSerializer(typeOracle, customFieldSerializerName);
    }

    /**
     * Prefixes to check. First see if we can find the custom serializer in the class's own package, then
     * check the updated gwt-rpc package, then fall back to the legacy com.google.gwt package.
     */
    private static final List<String> customFieldSerializerPrefixes = Arrays.asList(
            "",
            "org.gwtproject.rpc.core.",
            "com.google.gwt.user.client.rpc.core."
    );

    /**
     * Finds the custom field serializer for a given qualified source name.
     *
     * @param typeOracle
     * @param customFieldSerializerName
     * @return the custom field serializer for a type of <code>null</code> if
     *         there is not one
     */
    public static TypeElement findCustomFieldSerializer(SerializingTypes typeOracle,
                                                       String customFieldSerializerName) {
        for (String prefix : customFieldSerializerPrefixes) {
            TypeElement customSerializer = typeOracle.getElements().getTypeElement(prefix + customFieldSerializerName);
            if (customSerializer != null) {
                return customSerializer;
            }
        }

        return null;
    }

    /**
     * Returns the name for a custom field serializer, given a source name.
     *
     * @param sourceName
     * @return the custom field serializer type name for a given source name.
     */
    public static String getCustomFieldSerializerName(TypeMirror sourceName) {
        return ClassName.get(sourceName) + "_CustomFieldSerializer";
    }

    //not totally sure about this, but it seems to be correct
    static TypeMirror getBaseType(Types types, TypeMirror type) {
        return types.erasure(type);
//        if (type.isParameterized() != null) {
//            return type.isParameterized().getBaseType();
//        } else if (type.isRawType() != null) {
//            return type.isRawType().getBaseType();
//        }
//
//        return (JRealClassType) type;
    }

    private static boolean hasGwtTransientAnnotation(VariableElement field) {
        for (AnnotationMirror a : field.getAnnotationMirrors()) {
            if (a.getAnnotationType().asElement().getSimpleName().toString().equals("GwtTransient")) {
                return true;
            }
        }
        return false;
    }

//    /**
//     * @param type the type to query
//     * @return true if the type is annotated with @PersistenceCapable(...,
//     *         detachable="true")
//     */
//    static boolean hasJdoAnnotation(Element type) {
//        if (JDO_PERSISTENCE_CAPABLE_ANNOTATION == null) {
//            return false;
//        }
//        Annotation annotation = type.getAnnotation(JDO_PERSISTENCE_CAPABLE_ANNOTATION);
//        if (annotation == null) {
//            return false;
//        }
//        try {
//            Object value = JDO_PERSISTENCE_CAPABLE_DETACHABLE_METHOD.invoke(annotation, (Object[]) null);
//            if (value instanceof String) {
//                return "true".equalsIgnoreCase((String) value);
//            } else {
//                return false;
//            }
//        } catch (IllegalAccessException e) {
//            // will return false
//        } catch (InvocationTargetException e) {
//            // will return false
//        }
//
//        return false;
//    }

//    /**
//     * @param type the type to query
//     * @return true if the type is annotated with @Entity
//     */
//    static boolean hasJpaAnnotation(Element type) {
//        if (JPA_ENTITY_ANNOTATION == null) {
//            return false;
//        }
//        Annotation annotation = type.getAnnotation(JPA_ENTITY_ANNOTATION);
//        return annotation != null;
//    }

    static boolean isAutoSerializable(TypeMirror type, SerializingTypes types) {
        try {
            TypeMirror isSerializable = getIsSerializableMarkerInterface(types).asType();
            if (types.getTypes().isAssignable(type, isSerializable)) {
                return true;
            }
        } catch (NotFoundException ignore) {
        }
        try {
            TypeMirror serializable = getSerializableMarkerInterface(types).asType();
            if (types.getTypes().isAssignable(type, serializable)) {
                return true;
            }
        } catch (NotFoundException ignore) {
        }
        return false;
    }

    /**
     * Returns <code>true</code> if this type is part of the standard java
     * packages.
     */
    static boolean isInStandardJavaPackage(String className) {
        if (className.startsWith("java.")) {
            return true;
        }

        if (className.startsWith("javax.")) {
            return true;
        }

        return false;
    }

    static void recordTypeParametersIn(TypeMirror type, Set<TypeVariable> params) {
//        JTypeParameter isTypeParameter = type.isTypeParameter();
        if (type.getKind() == TypeKind.TYPEVAR) {
            params.add((TypeVariable) type);
        }

        if (type.getKind() == TypeKind.ARRAY) {
            recordTypeParametersIn(((ArrayType) type).getComponentType(), params);
        }

        if (type.getKind() == TypeKind.WILDCARD) {
            TypeMirror extendsBound = ((WildcardType) type).getExtendsBound();
            if (extendsBound != null) {
                recordTypeParametersIn(extendsBound, params);
            }
        }

        //TODO I'm about 70% sure this is a bad idea in general, to use type params as if they were fields...
        if (type.getKind() == TypeKind.DECLARED) {
            for (TypeMirror arg : ((DeclaredType) type).getTypeArguments()) {
                recordTypeParametersIn(arg, params);
            }
        }
    }

    /**
     * Return <code>true</code> if a class's fields should be considered for
     * serialization. If it returns <code>false</code> then none of the fields of
     * this class should be serialized.
     */
    static boolean shouldConsiderFieldsForSerialization(SerializingTypes types, TypeMirror type, TypeFilter filter,
                                                        ProblemReport problems) {
        if (!isAllowedByFilter(filter, type, problems)) {
            return false;
        }

        if (!isDeclaredSerializable(type, types)) {
            problems.add(type, ClassName.get(type) + " is not assignable to '"
                    /*+ IsSerializable.class.getName() + "' or '"*/ + Serializable.class.getName()
                    + "' nor does it have a custom field serializer", ProblemReport.Priority.DEFAULT);
            return false;
        }

        if (isManuallySerializable(types, type)) {
            TypeElement manualSerializer = findCustomFieldSerializer(types, type);
            assert (manualSerializer != null);

            List<String> fieldProblems = CustomFieldSerializerValidator.validate(types.getTypes(), manualSerializer, type);
            if (!fieldProblems.isEmpty()) {
                for (String problem : fieldProblems) {
                    problems.add(type, problem, ProblemReport.Priority.FATAL);
                }
                return false;
            }
        } else {
            assert (isAutoSerializable(type, types));

            if (!isAccessibleToSerializer(types, type)) {
                // Class is not visible to a serializer class in the same package
                problems.add(type, ClassName.get(type)
                        + " is not accessible from a class in its same package; it "
                        + "will be excluded from the set of serializable types", ProblemReport.Priority.DEFAULT);
                return false;
            }
            TypeElement typeElt = (TypeElement) types.getTypes().asElement(type);
            if (typeElt.getEnclosingElement() instanceof TypeElement && !typeElt.getModifiers().contains(Modifier.STATIC)) {
                // Non-static member types cannot be serialized
                problems.add(type, ClassName.get(type) + " is nested but "
                                + "not static; it will be excluded from the set of serializable " + "types",
                        ProblemReport.Priority.DEFAULT);
                return false;
            }
        }

        return true;
    }

    /**
     * Returns <code>true</code> if the field qualifies for serialization without
     * considering its type.
     */
    public static boolean shouldConsiderForSerialization(VariableElement field) {
        if (field.getModifiers().contains(Modifier.STATIC)
                || field.getModifiers().contains(Modifier.TRANSIENT)
                || hasGwtTransientAnnotation(field)) {
            return false;
        }

        //in this new impl, we always serialize final fields
//        if (field.isFinal() && !Shared.shouldSerializeFinalFields(logger, context)) {
//            logFinalField(logger, context, field);
//            return false;
//        }
        return true;
    }

//    private static void logFinalField(TreeLogger logger, GeneratorContext context, JField field) {
//        TreeLogger.Type logLevel;
//        if (Shared.shouldSuppressNonStaticFinalFieldWarnings(logger, context)) {
//            logLevel = TreeLogger.DEBUG;
//        } else if (isManuallySerializable(field.getEnclosingType())) {
//            // If the type has a custom serializer, assume the programmer knows best.
//            logLevel = TreeLogger.DEBUG;
//        } else {
//            logLevel = TreeLogger.WARN;
//        }
//        logger.branch(logLevel, "Field '" + field + "' will not be serialized because it is final");
//    }

    private static boolean directlyImplementsMarkerInterface(SerializingTypes types, TypeMirror type) {
        TypeElement typeElt = (TypeElement) types.getTypes().asElement(type);
        try {
            if (TypeHierarchyUtils.directlyImplementsInterface(types.getTypes(), typeElt, getIsSerializableMarkerInterface(types).asType())) {
                return true;
            }
        } catch (NotFoundException ignore) {
        }
        try {
            if (TypeHierarchyUtils.directlyImplementsInterface(types.getTypes(), typeElt,getSerializableMarkerInterface(types).asType())) {
                return true;
            }
        } catch (NotFoundException ignore) {
        }
        return false;
    }

    private static ArrayType getArrayType(Types types, int rank, TypeMirror component) {
        assert (rank > 0);

        ArrayType array = null;
        TypeMirror currentComponent = component;
        for (int i = 0; i < rank; ++i) {
            array = types.getArrayType(currentComponent);
            currentComponent = array;
        }

        return array;
    }

    private static TypeElement getIsSerializableMarkerInterface(SerializingTypes types) throws NotFoundException {
        throw new NotFoundException();
    }

    private static TypeElement getSerializableMarkerInterface(SerializingTypes types) throws NotFoundException {
        TypeElement typeElement = types.getElements().getTypeElement(Serializable.class.getName());
        if (typeElement == null) {
            throw new NotFoundException();
        }
        return typeElement;
    }

    /**
     * Returns <code>true</code> if a serializer class could access this type.
     */
    private static boolean isAccessibleToSerializer(SerializingTypes types, TypeMirror type) {
        Element typeElt = types.getTypes().asElement(type);
        if (typeElt.getModifiers().contains(Modifier.PRIVATE)) {
            return false;
        }

        if (isInStandardJavaPackage(ClassName.get(type).toString())) {
            if (!typeElt.getModifiers().contains(Modifier.PUBLIC)) {
                return false;
            }
        }

        if (typeElt.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            return isAccessibleToSerializer(types, typeElt.getEnclosingElement().asType());
        }

        return true;
    }

    private static boolean isAllowedByFilter(TypeFilter filter, TypeMirror classType,
                                             ProblemReport problems) {
        if (!filter.isAllowed(classType)) {
            problems.add(classType, ClassName.get(classType)
                    + " is excluded by type filter ", ProblemReport.Priority.AUXILIARY);
            return false;
        }

        return true;
    }

    private static boolean isDeclaredSerializable(TypeMirror type, SerializingTypes types) {
        return isAutoSerializable(type, types) || isManuallySerializable(types, type);
    }

    private static boolean isDirectlySerializable(SerializingTypes types, TypeMirror type) {
        return directlyImplementsMarkerInterface(types, type) || isManuallySerializable(types, type);
    }

    private static boolean isManuallySerializable(SerializingTypes types, TypeMirror type) {
        return findCustomFieldSerializer(types, type) != null;
    }

//    private static void logSerializableTypes(TreeLogger logger, Set<JClassType> fieldSerializableTypes) {
//        if (!logger.isLoggable(TreeLogger.DEBUG)) {
//            return;
//        }
//        TreeLogger localLogger =
//                logger.branch(TreeLogger.DEBUG, "Identified " + fieldSerializableTypes.size()
//                        + " serializable type" + ((fieldSerializableTypes.size() == 1) ? "" : "s"), null);
//
//        for (JClassType fieldSerializableType : fieldSerializableTypes) {
//            localLogger.branch(TreeLogger.DEBUG, fieldSerializableType
//                    .getParameterizedQualifiedSourceName(), null);
//        }
//    }

    private boolean alreadyCheckedObject;

    /**
     * Cache of the {@link DeclaredType} for {@link Collection}.
     */
    private final DeclaredType collectionClass;

//    private Set<String> enhancedClasses = null;

    private PrintWriter logOutputWriter;

    /**
     * Cache of the {@link DeclaredType} for {@link Map}.
     */
    private final DeclaredType mapClass;

    private final Map<String, TypeMirror> rootTypes = new LinkedHashMap<>();

    private final TypeConstrainer typeConstrainer;
    private TypeFilter typeFilter = DEFAULT_TYPE_FILTER;

    private final TypeParameterExposureComputer typeParameterExposureComputer;

    /**
     * The set of type parameters that appear in one of the root types.
     * TODO(spoon): It would be cleaner to delete this field, and instead to have
     * @link #addRootType(TreeLogger, JType) replace parameters with wildcard
     * types. Then the root types would not contain any parameters.
     */
    private Set<TypeVariable> typeParametersInRootTypes = new HashSet<>();

    /**
     * Map of {@link TypeMirror} to {@link TypeInfoComputed}.
     */
    private final Map<String, TypeInfoComputed> typeToTypeInfoComputed =
            new HashMap<>();

    private final SerializingTypes types;

    private final Messager messager;

    /**
     * Constructs a builder.
     */
    public SerializableTypeOracleBuilder(Elements elements, Messager messager, SerializingTypes serializingTypes)
            /*throws UnableToCompleteException */{
        this.messager = messager;
        this.types = serializingTypes;
        this.typeParameterExposureComputer = new TypeParameterExposureComputer(this.types, typeFilter, messager);
        typeConstrainer = new TypeConstrainer(this.types);

        collectionClass = (DeclaredType) elements.getTypeElement(Collection.class.getName()).asType();
        mapClass = (DeclaredType) elements.getTypeElement(Map.class.getName()).asType();

//        enhancedClasses = Shared.getEnhancedTypes(context.getPropertyOracle());
    }

    public void addRootType(/*TreeLogger logger, */TypeMirror type) {
        if (type.getKind().isPrimitive()) {
            return;
        }

//        JClassType clazz = (JClassType) type;
        TypeName typeName = TypeName.get(type);
        if (!rootTypes.containsKey(typeName.toString())) {
            recordTypeParametersIn(type, typeParametersInRootTypes);

            rootTypes.put(typeName.toString(), type);
        } else {
//            if (logger.isLoggable(TreeLogger.TRACE)) {
//                logger.log(TreeLogger.TRACE, clazz.getParameterizedQualifiedSourceName()
//                        + " is already a root type.");
//            }
        }
    }

    /**
     * Builds a {@link SerializableTypeOracle} for a given set of root types.
     *
     * @return a {@link SerializableTypeOracle} for the specified set of root
     *         types
     *
     * @throws UnableToCompleteException if there was not at least one
     *           instantiable type assignable to each of the specified root types
     */
    public SerializableTypeOracle build(/*TreeLogger logger*/) throws UnableToCompleteException {
        alreadyCheckedObject = false;

        boolean allSucceeded = true;

        for (TypeMirror type : rootTypes.values()) {
            ProblemReport problems = new ProblemReport(messager);
            problems.setContextType(type);
            boolean entrySucceeded =
                    computeTypeInstantiability(type,
                            TypePaths.createRootPath(type), problems).hasInstantiableSubtypes();
            if (!entrySucceeded) {
                if (!problems.hasFatalProblems()) {
                    messager.printMessage(Kind.ERROR, ClassName.get(type).toString() + " has no instantiable subtypes");
//                    logger.log(TreeLogger.ERROR, "'" + entry.getKey().getQualifiedSourceName() +
//                            "' has no instantiable subtypes");
                } else {
                    problems.report(Kind.ERROR, Kind.NOTE);
                }
            } else {
                maybeReport(problems);
            }
            allSucceeded &= entrySucceeded & !problems.hasFatalProblems();
        }

        if (!allSucceeded) {
            throw new UnableToCompleteException();
        }
        assertNothingPending();

        // Add covariant arrays in a separate pass. We want to ensure that nothing is pending
        // so that the leaf type's instantiableTypes variable is ready (if it's computed at all)
        // and all of the leaf's subtypes are ready.
        // (Copy values to avoid concurrent modification.)
        List<TypeInfoComputed> ticsToCheck = new ArrayList<TypeInfoComputed>();
        ticsToCheck.addAll(typeToTypeInfoComputed.values());
        for (TypeInfoComputed tic : ticsToCheck) {
            if (tic.getType().getKind() == TypeKind.ARRAY && tic.instantiable) {
                ProblemReport problems = new ProblemReport(messager);
                problems.setContextType(tic.getType());

                markArrayTypes((ArrayType) tic.getType(), tic.getPath(), problems);

                maybeReport(problems);
                allSucceeded &= !problems.hasFatalProblems();
            }
        }

        if (!allSucceeded) {
            throw new UnableToCompleteException();
        }
        assertNothingPending();

        pruneUnreachableTypes();

        //TODO restore this, writing to an external log for manual review (if enabled)
//        logReachableTypes(logger);

        Set<TypeMirror> possiblyInstantiatedTypes = new TreeSet<>(JTYPE_COMPARATOR);

        Set<TypeMirror> fieldSerializableTypes = new TreeSet<>(JTYPE_COMPARATOR);

        for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
            if (tic.getType().getKind() != TypeKind.DECLARED && tic.getType().getKind() != TypeKind.ARRAY) {
                continue;
            }
            TypeMirror type = types.getTypes().erasure(tic.getType());

            if (ClassName.get(JTypeUtils.getLeafType(type)).toString().equals("java.lang.JsException")) {
                // JsException is not considered serializable since it is never available at JVM.
                continue;
            }

            TypeElement typeElt = (TypeElement) types.getTypes().asElement(type);
            if (tic.isInstantiable()) {
                assert (!typeElt.getModifiers().contains(Modifier.ABSTRACT) || typeElt.getKind() == ElementKind.ENUM);

                possiblyInstantiatedTypes.add(type);
            }

            if (tic.isFieldSerializable()) {
                assert (typeElt.getKind() != ElementKind.INTERFACE);

                fieldSerializableTypes.add(type);
            }

//            if (tic.maybeEnhanced()
//                    || (enhancedClasses != null && enhancedClasses.contains(type.getQualifiedSourceName()))) {
//                type.setEnhanced();
//            }
        }

//        logSerializableTypes(logger, fieldSerializableTypes);

        return new SerializableTypeOracleImpl(fieldSerializableTypes, possiblyInstantiatedTypes, types.getTypes());
    }

//    /**
//     * Set the {@link PrintWriter} which will receive a detailed log of the types
//     * which were examined in order to determine serializability.
//     */
//    public void setLogOutputWriter(PrintWriter logOutputWriter) {
//        this.logOutputWriter = logOutputWriter;
//    }

    public void setTypeFilter(TypeFilter typeFilter) {
        this.typeFilter = typeFilter;
        typeParameterExposureComputer.setTypeFilter(typeFilter);
    }

    /**
     * This method determines information about serializing a type with GWT. To do
     * so, it must traverse all subtypes as well as all field types of those
     * types, transitively.
     *
     * It returns a {@link TypeInfoComputed} with the information found.
     *
     * As a side effect, all types needed--plus some--to serialize this type are
     * accumulated in {@link #typeToTypeInfoComputed}. In particular, there will
     * be an entry for any type that has been validated by this method, as a
     * shortcircuit to avoid recomputation.
     *
     * The method is exposed using default access to enable testing.
     */
    TypeInfoComputed computeTypeInstantiability(/*TreeLogger logger, */final TypeMirror type, TypePath path,
                                                ProblemReport problems) {
        assert (type != null);
        if (type.getKind().isPrimitive()) {
            TypeInfoComputed tic = ensureTypeInfoComputed(type, path);
            tic.setInstantiableSubtypes(true);
            tic.setInstantiable(false);
            return tic;
        }

//        assert (type instanceof JClassType);

//        JClassType classType = (JClassType) type;

        TypeInfoComputed tic = typeToTypeInfoComputed.get(ClassName.get(type).toString());
        if (tic != null && tic.isDone()) {
            // we have an answer already; use it.
            return tic;
        }

//        TreeLogger localLogger =
//                logger.branch(TreeLogger.DEBUG, classType.getParameterizedQualifiedSourceName(), null);

        if (type.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVar = (TypeVariable) type;
            if (typeParametersInRootTypes.contains(typeVar)) {
                return computeTypeInstantiability(/*localLogger, */typeVar.getUpperBound(), TypePaths
                        .createTypeParameterInRootPath(path, typeVar), problems);
            }

      /*
       * This type parameter was not in a root type and therefore it is the
       * caller's responsibility to deal with it. We assume that it is
       * indirectly instantiable here.
       */
            tic = ensureTypeInfoComputed(type, path);
            tic.setInstantiableSubtypes(true);
            tic.setInstantiable(false);
            return tic;
        }

        if (type.getKind() == TypeKind.WILDCARD) {
            WildcardType wildcard = (WildcardType) type;
            boolean success = computeTypeInstantiability(/*localLogger, */wildcard.getExtendsBound(), path, problems)
                    .hasInstantiableSubtypes();
            tic = ensureTypeInfoComputed(type, path);
            tic.setInstantiableSubtypes(success);
            tic.setInstantiable(false);
            return tic;
        }

        if (type.getKind() == TypeKind.ARRAY) {
            TypeInfoComputed arrayTic = checkArrayInstantiable((ArrayType) type, path, problems);
            assert typeToTypeInfoComputed.get(ClassName.get(type).toString()) != null;
            return arrayTic;
        }

        if (types.getTypes().isSameType(type, types.getJavaLangObject().asType())) {
        /*
         * Report an error if the type is or erases to Object since this violates
         * our restrictions on RPC. Should be fatal, but I worry users may have
         * Object-using code they can't readily get out of the class hierarchy.
         */
            problems.add(type, "In order to produce smaller client-side code, 'Object' is not "
                    + "allowed; please use a more specific type", ProblemReport.Priority.DEFAULT);
            tic = ensureTypeInfoComputed(type, path);
            tic.setInstantiable(false);
            return tic;
        }

        //TODO warn here
//        if (classType.isRawType() != null) {
//            localLogger
//                    .log(
//                            TreeLogger.DEBUG,
//                            "Type '"
//                                    + classType.getQualifiedSourceName()
//                                    + "' should be parameterized to help the compiler produce the smallest code size possible for your module",
//                            null);
//        }


        // TreeLogger subtypesLogger = localLogger.branch(TreeLogger.DEBUG,
        // "Analyzing subclasses:", null);
        tic = ensureTypeInfoComputed(type, path);
        Set<TypeMirror> instantiableTypes = new HashSet<>();
        boolean anySubtypes =
                checkSubtypes(types, type, instantiableTypes, path, problems);
        if (!tic.isDone()) {
            tic.setInstantiableSubtypes(anySubtypes);
            tic.setInstantiable(false);
        }
        // Don't publish this until complete to ensure nobody depends on partial results.
        tic.instantiableTypes = instantiableTypes;
        return tic;
    }

    int getTypeParameterExposure(DeclaredType type, int index) {
        return getFlowInfo(type, index).getExposure();
    }

    /**
     * Returns <code>true</code> if the fields of the type should be considered
     * for serialization.
     *
     * Default access to allow for testing.
     */
    boolean shouldConsiderFieldsForSerialization(TypeMirror type, ProblemReport problems) {
        return shouldConsiderFieldsForSerialization(types, type, typeFilter, problems);
    }

    private void assertNothingPending() {
        if (getClass().desiredAssertionStatus()) {
            for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
                assert (!tic.isPendingInstantiable());
            }
        }
    }

    /**
     * Consider any subtype of java.lang.Object which qualifies for serialization.
     */
    private void checkAllSubtypesOfObject(/*TreeLogger logger, */TypePath parent, ProblemReport problems) {
        if (alreadyCheckedObject) {
            return;
        }
        alreadyCheckedObject = true;

    /*
     * This will pull in the world and the set of serializable types will be
     * larger than it needs to be. We exclude types that do not qualify for
     * serialization to avoid generating false errors due to types that do not
     * qualify for serialization and have no serializable subtypes.
     */
//        TreeLogger localLogger =
//                logger.branch(TreeLogger.WARN,
//                        "Checking all subtypes of Object which qualify for serialization", null);
        List<TypeMirror> allTypes = types.getSubtypes(types.getJavaLangObject()).stream().map(Element::asType).collect(Collectors.toList());
        for (TypeMirror cls : allTypes) {
            if (isDeclaredSerializable(cls, types)) {
                computeTypeInstantiability(cls, TypePaths.createSubtypePath(parent, cls,
                        types.getJavaLangObject().asType()), problems);
            }
        }
    }

    private TypeInfoComputed checkArrayInstantiable(/*TreeLogger logger, */ArrayType array,
                                                    TypePath path, ProblemReport problems) {

        TypeMirror leafType = JTypeUtils.getLeafType(array);
        if (leafType.getKind() == TypeKind.WILDCARD) {
            WildcardType wild = (WildcardType) leafType;
            ArrayType arrayType = getArrayType(types.getTypes(), JTypeUtils.getRank(array), wild.getExtendsBound());
            return checkArrayInstantiable(arrayType, path, problems);
        }

        TypeInfoComputed tic = ensureTypeInfoComputed(array, path);
        if (tic.isDone() || tic.isPendingInstantiable()) {
            return tic;
        }
        tic.setPendingInstantiable();

        if (leafType.getKind() == TypeKind.TYPEVAR && !typeParametersInRootTypes.contains(leafType)) {
            // Don't deal with non root type parameters, but make a TIC entry to
            // save time if it recurs. We assume they're indirectly instantiable.
            tic.setInstantiableSubtypes(true);
            tic.setInstantiable(false);
            return tic;
        }

        if (!isAllowedByFilter(array, problems)) {
            // Don't deal with filtered out types either, but make a TIC entry to
            // save time if it recurs. We assume they're not instantiable.
            tic.setInstantiable(false);
            return tic;
        }

        // An array is instantiable provided that any leaf subtype is instantiable.
        // (Ignores the possibility of empty arrays of non-instantiable types.)

//        TreeLogger branch = logger.branch(TreeLogger.DEBUG, "Analyzing component type:", null);

        TypeInfoComputed leafTic =
                computeTypeInstantiability(leafType, TypePaths
                        .createArrayComponentPath(array, path), problems);
        boolean succeeded = leafTic.hasInstantiableSubtypes();

        tic.setInstantiable(succeeded);
        return tic;
    }

    /**
     * Returns <code>true</code> if the declared fields of this type are all
     * instantiable. As a side-effect it fills in {@link TypeInfoComputed} for all
     * necessary types.
     */
    private boolean checkDeclaredFields(/*TreeLogger logger, */TypeInfoComputed typeInfo,
                                        TypePath parent, ProblemReport problems) {

        DeclaredType classOrInterface = (DeclaredType) typeInfo.getType();
        if (types.getTypes().asElement(classOrInterface).getKind() == ElementKind.ENUM) {
            // The fields of an enum are never serialized; they are always okay.
            return true;
        }

        TypeMirror baseType = getBaseType(types.getTypes(), classOrInterface);

        boolean allSucceeded = true;
        // TODO: Propagating the constraints will produce better results as long
        // as infinite expansion can be avoided in the process.
        List<VariableElement> fields = ElementFilter.fieldsIn(types.getTypes().asElement(baseType).getEnclosedElements());
        if (!fields.isEmpty()) {
//            TreeLogger localLogger =
//                    logger.branch(TreeLogger.DEBUG, "Analyzing the fields of type '"
//                            + classOrInterface.getParameterizedQualifiedSourceName()
//                            + "' that qualify for serialization", null);

            for (VariableElement field : fields) {
                if (!shouldConsiderForSerialization(/*localLogger, *//*context, */field)) {
                    continue;
                }

//                TreeLogger fieldLogger = localLogger.branch(TreeLogger.DEBUG, field.toString(), null);
                TypeMirror fieldType = field.asType();

                TypePath path = TypePaths.createFieldPath(parent, field);
                if (typeInfo.isManuallySerializable()
                        && JTypeUtils.getLeafType(fieldType) == types.getJavaLangObject()) {
                    checkAllSubtypesOfObject(/*fieldLogger.branch(TreeLogger.WARN,
                            "Object was reached from a manually serializable type", null),*/ path, problems);
                } else {
                    allSucceeded &=
                            computeTypeInstantiability(/*fieldLogger, */fieldType, path, problems)
                                    .hasInstantiableSubtypes();
                }
            }
        }

        boolean succeeded = allSucceeded || typeInfo.isManuallySerializable();
        if (succeeded) {
            typeInfo.setFieldSerializable();
        }

        return succeeded;
    }

    private boolean checkSubtype(TypeMirror classOrInterface,
                                 TypeMirror originalType, TypePath parent, ProblemReport problems) {
        if (types.getTypes().asElement(classOrInterface).getKind() == ElementKind.ENUM) {
            // The fields of an enum are never serialized; they are always okay.
            return true;
        }


        if (classOrInterface.getKind() == TypeKind.DECLARED) {
            if (isRawMapOrRawCollection(classOrInterface)) {
            /*
             * Backwards compatibility. Raw collections or maps force all object
             * subtypes to be considered.
             */
            //TODO removed, see comment in isRawMapOrRawCollection
//                checkAllSubtypesOfObject(/*logger, */parent, problems);
            } else {
//                TreeLogger paramsLogger =
//                        logger.branch(TreeLogger.DEBUG, "Checking parameters of '"
//                                + isParameterized.getParameterizedQualifiedSourceName() + "'");

                int i = 0;
                DeclaredType declaredType = (DeclaredType) classOrInterface;
                DeclaredType genericType = (DeclaredType) declaredType.asElement().asType();
                for (TypeMirror param : declaredType.getTypeArguments()) {
                    if (!checkTypeArgument(genericType, i, param, parent, problems)) {
                        return false;
                    }
                    i++;
                }
            }
        }

        // Check all super type fields first (recursively).
        TypeMirror superType = ((TypeElement) types.getTypes().asElement(classOrInterface)).getSuperclass();//TODO this is probably wrong, needs to not lose the generics as we go up
        if (superType.getKind() == TypeKind.NONE) {
            superType = null;
        }
        if (superType != null) {
            DeclaredType declaredType = (DeclaredType) superType;
            if (declaredType.getTypeArguments().size() != ((DeclaredType) declaredType.asElement().asType()).getTypeArguments().size()) {
                declaredType = JTypeUtils.asParameterizedByWildcards(types.getTypes(), declaredType);
                superType = declaredType;
            }

            if (isDeclaredSerializable(superType, types)) {
                superType = constrainTypeBy(superType, originalType);
                if (superType == null) {
                    return false;
                }

                boolean superTypeOk = false;
                superTypeOk =
                        checkSubtype(superType, originalType, TypePaths.createSupertypePath(parent,
                                superType, classOrInterface), problems);

                /*
                 * If my super type did not check out, then I am not instantiable and we
                 * should error out... UNLESS I am directly serializable myself, in which
                 * case it's ok for me to be the root of a new instantiable hierarchy.
                 */
                if (!superTypeOk && !isDirectlySerializable(types, classOrInterface)) {
                    return false;
                }
            }
        }

        TypeInfoComputed tic = ensureTypeInfoComputed(classOrInterface, parent);
        return checkDeclaredFields(/*logger, */tic, parent, problems);
    }

    /**
     * Returns <code>true</code> if this type or one of its subtypes is
     * instantiable relative to a known base type.
     */
    private boolean checkSubtypes(SerializingTypes types, TypeMirror originalType,
                                  Set<TypeMirror> instSubtypes, TypePath path, ProblemReport problems) {
        TypeMirror baseType = getBaseType(types.getTypes(), originalType);
//        TreeLogger computationLogger =
//                logger.branch(TreeLogger.DEBUG, "Finding possibly instantiable subtypes");
        List<TypeMirror> candidates =
                getPossiblyInstantiableSubtypes(baseType, problems);
        boolean anySubtypes = false;

//        TreeLogger verificationLogger = logger.branch(TreeLogger.DEBUG, "Verifying instantiability");
        for (TypeMirror candidate : candidates) {
            if (getBaseType(types.getTypes(), candidate) == baseType && !JTypeUtils.isRawType(originalType)) {
                // Don't rely on the constrainer when we have perfect information.
                candidate = originalType;
            } else {
                candidate = constrainTypeBy(candidate, originalType);
                if (candidate == null) {
                    continue;
                }
            }

            if (!isAllowedByFilter(candidate, problems)) {
                continue;
            }

            TypePath subtypePath = TypePaths.createSubtypePath(path, candidate, originalType);
            TypeInfoComputed tic = ensureTypeInfoComputed(candidate, subtypePath);
            if (tic.isDone()) {
                if (tic.isInstantiable()) {
                    anySubtypes = true;
                    instSubtypes.add(candidate);
                }
                continue;
            } else if (tic.isPendingInstantiable()) {
                anySubtypes = true;
                instSubtypes.add(candidate);
                continue;
            }
            tic.setPendingInstantiable();

//            TreeLogger subtypeLogger =
//                    verificationLogger.branch(TreeLogger.DEBUG, candidate
//                            .getParameterizedQualifiedSourceName());
            boolean instantiable =
                    checkSubtype(/*subtypeLogger, */candidate, originalType, subtypePath, problems);
            anySubtypes |= instantiable;
            tic.setInstantiable(instantiable);

//            if (instantiable) {
//                subtypeLogger.branch(TreeLogger.DEBUG, "Is instantiable");
//            }

            // Note we are leaving hasInstantiableSubtypes() as false which might be
            // wrong but it is only used by arrays and thus it will never be looked at
            // for this tic.
            if (instantiable) {
                instSubtypes.add(candidate);
            }
        }

        return anySubtypes;
    }

    /**
     * Check the argument to a parameterized type to see if it will make the type
     * it is applied to be serializable. As a side effect, populates
     * {@link #typeToTypeInfoComputed} in the same way as
     * {@link #computeTypeInstantiability}.
     *
     * @param baseType - The generic type the parameter is on
     * @param paramIndex - The index of the parameter in the generic type
     * @param typeArg - An upper bound on the actual argument being applied to the
     *          generic type
     *
     * @return Whether the a parameterized type can be serializable if
     *         <code>baseType</code> is the base type and the
     *         <code>paramIndex</code>th type argument is a subtype of
     *         <code>typeArg</code>.
     */
    private boolean checkTypeArgument(/*TreeLogger logger, */DeclaredType baseType, int paramIndex,
                                      TypeMirror typeArg, TypePath parent, ProblemReport problems) {
        if (typeArg.getKind() == TypeKind.WILDCARD) {
            WildcardType isWildcard = (WildcardType) typeArg;
            return checkTypeArgument(baseType, paramIndex, isWildcard.getExtendsBound(), parent,
                    problems);
        }

        if (typeArg.getKind() == TypeKind.ARRAY) {
//            ArrayType typeArgAsArray = (ArrayType) typeArg;
//            if (JTypeUtils.getLeafType(typeArgAsArray).getKind() == TypeKind.TYPEVAR) {
//                TypeMirror parameterOfTypeArgArray = JTypeUtils.getLeafType(typeArgAsArray);
//                TypeMirror declaringClass = parameterOfTypeArgArray.getDeclaringClass();
//                if (declaringClass != null) {
//                    TypeParameterFlowInfo flowInfoForArrayParam =
//                            getFlowInfo(declaringClass, parameterOfTypeArgArray.getOrdinal());
//                    TypeParameterFlowInfo otherFlowInfo = getFlowInfo(baseType, paramIndex);
//                    if (otherFlowInfo.getExposure() >= 0
//                            && otherFlowInfo.isTransitivelyAffectedBy(flowInfoForArrayParam)) {
//                        problems.add(baseType, "Cannot serialize type '"
//                                        + baseType.getParameterizedQualifiedSourceName()
//                                        + "' when given an argument of type '"
//                                        + typeArg.getParameterizedQualifiedSourceName()
//                                        + "' because it appears to require serializing arrays " + "of unbounded dimension",
//                                Priority.DEFAULT);
//                        return false;
//                    }
//                }
//            }
            //TODO not sure quite how to handle this yet
            assert false : "Don't yet understand this...";
            return false;
        }

        TypePath path = TypePaths.createTypeArgumentPath(parent, baseType, paramIndex, typeArg);
        int exposure = getTypeParameterExposure(baseType, paramIndex);
        switch (exposure) {
            case TypeParameterExposureComputer.EXPOSURE_DIRECT: {
//                TreeLogger branch =
//                        logger.branch(TreeLogger.DEBUG, "Checking type argument " + paramIndex + " of type '"
//                                + baseType.getParameterizedQualifiedSourceName()
//                                + "' because it is directly exposed in this type or in one of its subtypes");
                return computeTypeInstantiability(/*branch, */typeArg, path, problems)
                        .hasInstantiableSubtypes()
                        || mightNotBeExposed(baseType, paramIndex);
            }
            case TypeParameterExposureComputer.EXPOSURE_NONE:
                // Ignore this argument
//                logger.log(TreeLogger.DEBUG, "Ignoring type argument " + paramIndex + " of type '"
//                        + baseType.getParameterizedQualifiedSourceName()
//                        + "' because it is not exposed in this or any subtype");
                return true;

            default: {
                assert (exposure >= TypeParameterExposureComputer.EXPOSURE_MIN_BOUNDED_ARRAY);
                problems.add(getArrayType(types.getTypes(), exposure, typeArg), "Checking type argument "
                        + paramIndex + " of type '" + ClassName.get(baseType)
                        + "' because it is exposed as an array with a maximum dimension of " + exposure
                        + " in this type or one of its subtypes", ProblemReport.Priority.AUXILIARY);
                return computeTypeInstantiability(/*logger, */getArrayType(types.getTypes(), exposure, typeArg),
                        path, problems).hasInstantiableSubtypes()
                        || mightNotBeExposed(baseType, paramIndex);
            }
        }
    }

    /**
     * If <code>type</code>'s base class does not inherit from
     * <code>superType</code>'s base class, then return <code>type</code>
     * unchanged. If it does, then return a subtype of <code>type</code> that
     * includes all values in both <code>type</code> and <code>superType</code>.
     * If there are definitely no such values, return <code>null</code>.
     */
    private TypeMirror constrainTypeBy(TypeMirror type, TypeMirror superType) {
        //TODO actually make this class work
        return typeConstrainer.constrainTypeBy((DeclaredType) type, (DeclaredType) superType);
    }

    private TypeParameterExposureComputer.TypeParameterFlowInfo getFlowInfo(DeclaredType type, int index) {
        return typeParameterExposureComputer.computeTypeParameterExposure(type, index);
    }

    /**
     * Returns the subtypes of a given base type as parameterized by wildcards.
     */
    private List<TypeMirror> getPossiblyInstantiableSubtypes(TypeMirror baseType, ProblemReport problems) {
        assert types.getTypes().isSameType(baseType, getBaseType(types.getTypes(), baseType));

        List<TypeMirror> possiblyInstantiableTypes = new ArrayList<TypeMirror>();
        if (types.getTypes().asElement(baseType).equals(types.getElements().getTypeElement(Object.class.getName()))) {
            return possiblyInstantiableTypes;
        }

        List<TypeMirror> candidates = new ArrayList<TypeMirror>();
        candidates.add(baseType);
        List<TypeMirror> subtypes = types.getSubtypes(baseType).stream().map(Element::asType).collect(Collectors.toList());
        candidates.addAll(subtypes);

        for (TypeMirror subtype : candidates) {
            TypeMirror subtypeBase = getBaseType(types.getTypes(), subtype);
            if (maybeInstantiable(types, subtypeBase, problems)) {
        /*
         * Convert the generic type into a parameterization that only includes
         * wildcards.
         */
//                JGenericType isGeneric = subtype.isGenericType();
//                if (isGeneric != null) {
//                    subtype = isGeneric.asParameterizedByWildcards();
//                } else {
//                    assert (subtype instanceof JRealClassType);
//                }
                subtype = JTypeUtils.asParameterizedByWildcards(types.getTypes(), (DeclaredType) subtype);

                possiblyInstantiableTypes.add(subtype);
            }
        }

        if (possiblyInstantiableTypes.size() == 0) {
            String possibilities[] = new String[candidates.size()];
            for (int i = 0; i < possibilities.length; i++) {
                TypeMirror subtype = candidates.get(i);
                String worstMessage = problems.getWorstMessageForType(subtype);
                if (worstMessage == null) {
                    possibilities[i] =
                            "   subtype " + ClassName.get(subtype)
                                    + " is not instantiable";
                } else {
                    // message with have the form "FQCN some-problem-description"
                    possibilities[i] = "   subtype " + worstMessage;
                }
            }
            problems.add(baseType, ClassName.get(baseType)
                    + " has no available instantiable subtypes.", ProblemReport.Priority.DEFAULT, possibilities);
        }
        return possiblyInstantiableTypes;
    }

    private TypeInfoComputed ensureTypeInfoComputed(TypeMirror type, TypePath path) {
        TypeInfoComputed tic = typeToTypeInfoComputed.get(ClassName.get(type).toString());
        if (tic == null) {
            tic = new TypeInfoComputed(type, path, types);
            typeToTypeInfoComputed.put(ClassName.get(type).toString(), tic);
        }
        return tic;
    }

    private boolean isAllowedByFilter(TypeMirror classType, ProblemReport problems) {
        return isAllowedByFilter(typeFilter, classType, problems);
    }

    /**
     * Returns <code>true</code> if the type is Collection<? extends Object> or
     * Map<? extends Object, ? extends Object>.
     */
    private boolean isRawMapOrRawCollection(TypeMirror type) {
        //TODO this is completely wrong, see javadoc for isSameType, and rethink even allowing this sort of crap
//        if (types.getTypes().isSameType(JTypeUtils.asParameterizationOf(types.getTypes(), type, collectionClass), JTypeUtils.asParameterizedByWildcards(types.getTypes(), collectionClass))) {
//            return true;
//        }
//
//        if (types.getTypes().isSameType(JTypeUtils.asParameterizationOf(types.getTypes(), type, mapClass), JTypeUtils.asParameterizedByWildcards(types.getTypes(), mapClass))) {
//            return true;
//        }

        return false;
    }

//    private void logPath(TreeLogger logger, TypePath path) {
//        if (path == null) {
//            return;
//        }
//
//        if (logger.isLoggable(TreeLogger.DEBUG)) {
//            logger.log(TreeLogger.DEBUG, path.toString());
//        }
//        logPath(logger, path.getParent());
//    }

//    private void logReachableTypes(TreeLogger logger) {
//        if (!context.isProdMode() && !logger.isLoggable(TreeLogger.DEBUG)) {
//            return;
//        }
//
//        if (logOutputWriter != null) {
//            // Route the TreeLogger output to an output stream.
//            PrintWriterTreeLogger printWriterTreeLogger = new PrintWriterTreeLogger(logOutputWriter);
//            printWriterTreeLogger.setMaxDetail(TreeLogger.ALL);
//            logger = printWriterTreeLogger;
//        }
//
//        if (logger.isLoggable(TreeLogger.DEBUG)) {
//            logger.log(TreeLogger.DEBUG, "Reachable types computed on: " + new Date().toString());
//        }
//        Set<JType> keySet = typeToTypeInfoComputed.keySet();
//        JType[] types = keySet.toArray(new JType[0]);
//        Arrays.sort(types, JTYPE_COMPARATOR);
//
//        for (JType type : types) {
//            TypeInfoComputed tic = typeToTypeInfoComputed.get(ClassName.get(type).toString());
//            assert (tic != null);
//
//            TreeLogger typeLogger =
//                    logger.branch(TreeLogger.DEBUG, tic.getType().getParameterizedQualifiedSourceName());
//            TreeLogger serializationStatus = typeLogger.branch(TreeLogger.DEBUG, "Serialization status");
//            if (tic.isInstantiable()) {
//                serializationStatus.branch(TreeLogger.DEBUG, "Instantiable");
//            } else {
//                if (tic.isFieldSerializable()) {
//                    serializationStatus.branch(TreeLogger.DEBUG, "Field serializable");
//                } else {
//                    serializationStatus.branch(TreeLogger.DEBUG, "Not serializable");
//                }
//            }
//
//            TreeLogger pathLogger = typeLogger.branch(TreeLogger.DEBUG, "Path");
//
//            logPath(pathLogger, tic.getPath());
//            logger.log(TreeLogger.DEBUG, "");
//        }
//
//        if (logOutputWriter != null) {
//            logOutputWriter.flush();
//        }
//    }

    /**
     * Mark arrays of <code>leafType</code> as instantiable, for arrays of
     * dimension up to <code>maxRank</code>.
     */
    private void markArrayTypesInstantiable(TypeMirror leafType, int maxRank, TypePath path) {
        for (int rank = 1; rank <= maxRank; ++rank) {
            ArrayType covariantArray = getArrayType(types.getTypes(), rank, leafType);

            TypeInfoComputed covariantArrayTic = ensureTypeInfoComputed(covariantArray, path);
            covariantArrayTic.setInstantiable(true);
        }
    }

    /**
     * Marks all covariant and lesser-ranked arrays as instantiable for all leaf types between
     * the given array's leaf type and its instantiable subtypes. (Note: this adds O(S * R)
     * array types to the output where S is the number of subtypes and R is the rank.)
     * Prerequisite: The leaf type's tic and its subtypes must already be created.
     * @see #checkArrayInstantiable
     */
    private void markArrayTypes(/*TreeLogger logger, */ArrayType array, TypePath path,
                                ProblemReport problems) {
//        logger = logger.branch(TreeLogger.DEBUG, "Adding array types for " + array);

        TypeMirror leafType = array;
        int rank = 0;
        while (leafType.getKind() == TypeKind.ARRAY) {
            leafType = ((ArrayType) leafType).getComponentType();
            rank++;
        }

        if (leafType.getKind() == TypeKind.TYPEVAR) {
            if (typeParametersInRootTypes.contains((TypeVariable) leafType)) {
                leafType = ((TypeVariable)leafType).getUpperBound(); // to match computeTypeInstantiability
            } else {
                // skip non-root leaf parameters, to match checkArrayInstantiable
                return;
            }
        }

        TypeInfoComputed leafTic = typeToTypeInfoComputed.get(ClassName.get(leafType).toString());
        if (leafTic == null) {
            problems.add(array, "internal error: leaf type not computed: " +
                    ClassName.get(leafType), ProblemReport.Priority.FATAL);
            return;
        }

        if (leafType.getKind().isPrimitive()) {
            markArrayTypesInstantiable(leafType, rank, path);
            return;
        }

        TypeMirror baseClass = getBaseType(types.getTypes(), leafType);

//        TreeLogger covariantArrayLogger =
//                logger.branch(TreeLogger.DEBUG, "Covariant array types:");

        Set<TypeMirror> instantiableTypes = leafTic.instantiableTypes;
        if (instantiableTypes == null) {
            // The types are there (due to a supertype) but the Set wasn't computed, so compute it now.
            instantiableTypes = new HashSet<>();
            List<TypeMirror> candidates =
                    getPossiblyInstantiableSubtypes(baseClass, problems);
            for (TypeMirror candidate : candidates) {
                TypeInfoComputed tic = typeToTypeInfoComputed.get(ClassName.get(candidate).toString());
                if (tic != null && tic.instantiable) {
                    instantiableTypes.add(candidate);
                }
            }
        }
        for (TypeMirror instantiableType : TypeHierarchyUtils.getAllTypesBetweenRootTypeAndLeaves(types, baseClass, instantiableTypes)) {
            if (!isAccessibleToSerializer(types, instantiableType)) {
                // Skip types that are not accessible from a serializer
                continue;
            }

//            if (covariantArrayLogger.isLoggable(TreeLogger.DEBUG)) {
//                covariantArrayLogger.branch(TreeLogger.DEBUG, getArrayType(typeOracle, array.getRank(),
//                        instantiableType).getParameterizedQualifiedSourceName());
//            }

            markArrayTypesInstantiable(instantiableType, rank, path);
        }
    }

    private boolean maybeInstantiable(SerializingTypes types, TypeMirror type, ProblemReport problems) {
        boolean success =
                canBeInstantiated(types, type, problems) && shouldConsiderFieldsForSerialization(type, problems);
//        if (success) {
//            if (logger.isLoggable(TreeLogger.DEBUG)) {
//                logger.log(TreeLogger.DEBUG, type.getParameterizedQualifiedSourceName()
//                        + " might be instantiable");
//            }
//        }
        return success;
    }

    /**
     * Report problems if they are fatal or we're debugging.
     */
    private void maybeReport(/*TreeLogger logger, */ProblemReport problems) {
        if (problems.hasFatalProblems()) {
            problems.reportFatalProblems(/*logger, */Kind.ERROR);
        }
        // if entrySucceeded, there may still be "warning" problems, but too
        // often they're expected (e.g. non-instantiable subtypes of List), so
        // we log them at DEBUG.
        // TODO(fabbott): we could blacklist or graylist those types here, so
        // instantiation during code generation would flag them for us.
        problems.report(/*logger, */Kind.NOTE, Kind.NOTE);
    }

    private boolean mightNotBeExposed(DeclaredType baseType, int paramIndex) {
        TypeParameterExposureComputer.TypeParameterFlowInfo flowInfo = getFlowInfo(baseType, paramIndex);
        return flowInfo.getMightNotBeExposed() || isManuallySerializable(types, baseType);
    }

    /**
     * Remove serializable types that were visited due to speculative paths but
     * are not really needed for serialization.
     *
     * NOTE: This is currently much more limited than it should be. For example, a
     * path sensitive prune could remove instantiable types also.
     */
    private void pruneUnreachableTypes() {
    /*
     * Record all supertypes of any instantiable type, whether or not they are
     * field serialziable.
     */
        Set<TypeMirror> supersOfInstantiableTypes = new LinkedHashSet<>();
        for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
            if (!tic.isInstantiable()) {
                continue;
            }
            if (tic.getType().getKind() == TypeKind.DECLARED) {
                DeclaredType sup = (DeclaredType) types.getTypes().erasure(tic.getType());//JClassType.getErasedType
                while (sup != null) {
                    supersOfInstantiableTypes.add(types.getTypes().erasure(sup));
                    TypeMirror superclass = MoreTypes.asTypeElement(types.getTypes().erasure(sup)).getSuperclass();
                    
                    if (superclass.getKind() == TypeKind.NONE) {
                        break;
                    }
                    sup = (DeclaredType) superclass;
                }
            } else if (tic.getType().getKind() == TypeKind.ARRAY) {
                supersOfInstantiableTypes.add(tic.getType());
                supersOfInstantiableTypes.add(types.getJavaLangObject().asType());
            }
        }

    /*
     * Record any field serializable type that is not in the supers of any
     * instantiable type.
     */
        Set<TypeMirror> toKill = new LinkedHashSet<>();
        for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
            if (tic.isFieldSerializable()
                    && !supersOfInstantiableTypes.contains(types.getTypes().erasure(tic.getType()))) {
                toKill.add(tic.getType());
            }
        }

    /*
     * Remove any field serializable supers that cannot be reached from an
     * instantiable type.
     */
        for (TypeMirror type : toKill) {
            typeToTypeInfoComputed.remove(type);
        }
    }

}

package org.gwtproject.rpc.serial.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.gwtproject.rpc.gwtapt.JTypeUtils;
import org.gwtproject.rpc.serial.processor.CustomFieldSerializerValidator;
import org.gwtproject.rpc.serial.processor.SerializableTypeOracleBuilder;
import org.gwtproject.rpc.serial.processor.SerializableTypeOracleUnion;
import org.gwtproject.rpc.serial.processor.SerializingTypes;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SerializableTypeModel {

    public static class Property {
        private final ExecutableElement setter;
        private final ExecutableElement getter;

        Property(ExecutableElement setter, ExecutableElement getter) {
            this.setter = setter;
            this.getter = getter;
        }

        public ExecutableElement getSetter() {
            return setter;
        }

        public ExecutableElement getGetter() {
            return getter;
        }

        public TypeName getTypeName() {
            return TypeName.get(getter.getReturnType());
        }

        //readFoo/writeFoo in stream reader and stream writer
        public String getStreamMethodName() {
            return getStreamMethodSuffix(getter.getReturnType(), 0);
        }

    }
    public static String getStreamMethodSuffix(TypeMirror type, int rank) {
        if (rank > 0) {
            return "Object";
        }
        if (type.getKind().isPrimitive()) {
            return type.getKind().name().substring(0, 1) + type.getKind().name().substring(1).toLowerCase();
        } else if (ClassName.get(type).toString().equals("java.lang.String")) {
            return "String";
        } else {
            return "Object";
        }
    }

    public static class Field {
        private final VariableElement field;

        Field(VariableElement field) {
            this.field = field;
        }

        public VariableElement getField() {
            return field;
        }

        public TypeName getTypeName() {
            return TypeName.get(field.asType());
        }

        //readFoo/writeFoo in stream reader and stream writer
        public String getStreamMethodName() {
            return getStreamMethodSuffix(field.asType(), 0);
        }
    }

    private final SerializingTypes types;

    private final TypeMirror type;
    private final TypeElement customFieldSerializer;
    private final List<Property> properties;
    private final List<Field> fields;//TODO do we need a real field? and how do we handle private?

    private boolean serializable;
    private boolean maybeInstantiated;
    private ClassName fieldSerializer;
    private final ClassName superclassFieldSerializer;

    public static SerializableTypeModel array(TypeMirror serializableType, SerializableTypeOracleUnion bidiOracle, SerializingTypes types) {
        return new SerializableTypeModel(types, serializableType, null, Collections.emptyList(), Collections.emptyList(), true, true, getFieldSerializer(serializableType, bidiOracle, types), null);
    }

    public static SerializableTypeModel create(SerializingTypes types, TypeElement serializableType, Messager messager, boolean serializable, boolean maybeInstantiated, SerializableTypeOracleUnion bidiOracle) {
        TypeElement customFieldSerializer = SerializableTypeOracleBuilder.findCustomFieldSerializer(types, serializableType.asType());
        List<Property> properties = new ArrayList<>();
        List<Field> fields = new ArrayList<>();

        ClassName fieldSerializer = getFieldSerializer(serializableType.asType(), bidiOracle, types);
        if (serializableType.getKind() == ElementKind.ENUM || customFieldSerializer != null) {
            //nothing to serialize for enums, or custom field serializer will manage it
            return new SerializableTypeModel(types, serializableType.asType(), customFieldSerializer, Collections.emptyList(), Collections.emptyList(), serializable, maybeInstantiated, fieldSerializer, null);
        }

        //rules of STOB are to look for fields,
        // * then invoke as properties if we can (at least on the jvm, but let's be consistent),
        // * else fall back to accessible fields,
        // * and then violator pattern - TODO

        for (VariableElement field : ElementFilter.fieldsIn(serializableType.getEnclosedElements())) {
            if (!SerializableTypeOracleBuilder.shouldConsiderForSerialization(field)) {
                 continue;
            }
            //TODO also consider @ConstructorProperties to write values, and read from getter or field
            //TODO how about builders?

            ExecutableElement setter = setter(field);
            ExecutableElement getter = getter(field);
            if (getter != null && setter != null) {
                properties.add(new Property(setter, getter));
                continue;
            }

            if (!field.getModifiers().contains(Modifier.PRIVATE)) {
                //use it as a field, just assign it directly
                fields.add(new Field(field));
            } else {
                //TODO violator pattern
                messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Field " + field.getEnclosingElement() + "." + field + " is private and is missing either getter or setter", field);
                assert false : "field " + field + " is private";
            }
        }

        // We will delegate to our superclass, if it is serializable, so that it can handle its own fields
        TypeMirror superclass = serializableType.getSuperclass();
        ClassName superclassSerializer;
        if (superclass.getKind() == TypeKind.NONE) {
            superclassSerializer = null;
        } else if (!bidiOracle.isSerializable(superclass)) {
            superclassSerializer = null;
        } else {
            superclassSerializer = getFieldSerializer(superclass, bidiOracle, types);
        }

        return new SerializableTypeModel(types, serializableType.asType(), customFieldSerializer, properties, fields, serializable, maybeInstantiated, fieldSerializer, superclassSerializer);
    }

    private static ExecutableElement setter(VariableElement field) {
        return ElementFilter.methodsIn(field.getEnclosingElement().getEnclosedElements()).stream()
                .filter(m -> m.getParameters().size() == 1)
                .filter(m ->
                        m.getSimpleName().contentEquals("set" + capitalize(field.getSimpleName().toString()))
                        || field.getSimpleName().toString().startsWith("is") && m.getSimpleName().contentEquals("set" + field.getSimpleName().toString().substring("is".length()))
                )
                .findAny()
                .orElse(null);
    }
    private static ExecutableElement getter(VariableElement field) {
        return ElementFilter.methodsIn(field.getEnclosingElement().getEnclosedElements()).stream()
                .filter(m -> m.getParameters().isEmpty())
                .filter(m ->
                        m.getSimpleName().contentEquals("get" + capitalize(field.getSimpleName().toString()))
                        || m.getSimpleName().contentEquals("is" + capitalize(field.getSimpleName().toString()))
                        || m.getSimpleName().contentEquals(field.getSimpleName()) && field.getSimpleName().toString().startsWith("is")
//                        || m.getSimpleName().contentEquals("has" + capitalize(field.getSimpleName().toString()))
                )
                .findAny()
                .orElse(null);
    }

    private static String capitalize(String s) {
        if (s.length() == 1) {
            return s.toUpperCase();
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private static String arraySerializerPackage(TypeMirror componentType, SerializingTypes t) {
        String commonPackage = "org.gwtproject.rpc.serialization.api.emul";
        if (componentType.getKind().isPrimitive()) {
            return commonPackage;
        }
        assert componentType.getKind() == TypeKind.DECLARED;
        String packageName = t.getElements().getPackageOf(t.getTypes().asElement(componentType)).getQualifiedName().toString();
        if (packageName.startsWith("java")) {
            return commonPackage + packageName;
        }
        return packageName;
    }

    private static ClassName getFieldSerializer(TypeMirror type, SerializableTypeOracleUnion bidiOracle, SerializingTypes t) {
        if (type.getKind() == TypeKind.ARRAY) {
            //TODO no more null check
            if (bidiOracle == null) {
                return ClassName.get(
                        arraySerializerPackage(JTypeUtils.getLeafType(type), t),
                        arraySerializerName(JTypeUtils.getLeafType(type), JTypeUtils.getRank(type), t)
                );
            } else {
                return ClassName.get(
                        arraySerializerPackage(JTypeUtils.getLeafType(type), t),
                        arraySerializerName(JTypeUtils.getLeafType(type), JTypeUtils.getRank(type), t),
                        bidiOracle.getSpecificFieldSerializer(type)
                );
            }
        }
        assert type.getKind() == TypeKind.DECLARED;
        Element elt = t.getTypes().asElement(type);

        String packageName = t.getElements().getPackageOf(elt).getQualifiedName().toString();
        if (packageName.startsWith("java")) {
            packageName = "org.gwtproject.rpc.serialization.api.emul" + packageName;
        }
        String outerClassName = "FieldSerializer";
        do {
            outerClassName = elt.getSimpleName() + "_" + outerClassName;
            elt = elt.getEnclosingElement();
        } while (elt.getKind() != ElementKind.PACKAGE);
        //TODO no more null check
        if (bidiOracle == null) {
            return ClassName.get(packageName, outerClassName);
        } else {
            return ClassName.get(packageName, outerClassName, bidiOracle.getSpecificFieldSerializer(elt.asType()));
        }
    }

    private static String arraySerializerName(TypeMirror componentType, int rank, SerializingTypes t) {
        if (componentType.getKind().isPrimitive()) {
            return componentType.getKind().name() + "_ArrayRank_" + rank + "_FieldSerializer";
        }
        assert componentType.getKind() == TypeKind.DECLARED;
        return t.getTypes().asElement(componentType).getSimpleName() + "_ArrayRank_" + rank + "_FieldSerializer";
    }


    private SerializableTypeModel(SerializingTypes types, TypeMirror type, TypeElement customFieldSerializer, List<Property> properties, List<Field> fields, boolean serializable, boolean maybeInstantiated, ClassName fieldSerializer, ClassName superclassFieldSerializer) {
        this.types = types;
        this.type = type;
        this.customFieldSerializer = customFieldSerializer;
        this.properties = properties;
        this.fields = fields;
        this.serializable = serializable;
        this.maybeInstantiated = maybeInstantiated;
        this.fieldSerializer = fieldSerializer;
        this.superclassFieldSerializer = superclassFieldSerializer;
    }

    public TypeName getTypeName() {
        if (type.getKind() == TypeKind.DECLARED) {
            return ClassName.get(types.getTypes().erasure(type));
        }
        return ClassName.get(type);
    }

    public TypeElement getTypeElement() {
        return (TypeElement) types.getTypes().asElement(type);
    }

    public boolean isSerializable() {
        return serializable;
    }

    public boolean mayBeInstantiated() {
        return maybeInstantiated;
    }

    public ClassName getFieldSerializer() {
        return fieldSerializer;
    }

    public ClassName getSuperclassFieldSerializer() {
        return superclassFieldSerializer;
    }

    public TypeElement getCustomFieldSerializer() {
        return customFieldSerializer;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getFieldSerializerName() {
        assert type.getKind() == TypeKind.DECLARED : "Can't create field serializer name for " + type.getKind();
        Element element = types.getTypes().asElement(type);
        StringBuilder name = new StringBuilder("FieldSerializer");
        do {
            name.insert(0, element.getSimpleName() + "_");
            element = element.getEnclosingElement();
        } while (element.getKind() != ElementKind.PACKAGE);

        return name.toString();
    }
    public String getFieldSerializerPackage() {
        assert type.getKind() == TypeKind.DECLARED : "Can't create field serializer name for " + type.getKind();
        Element element = types.getTypes().asElement(type);
        String packageName = types.getElements().getPackageOf(element).getQualifiedName().toString();
        if (packageName.startsWith("java")) {
            return "org.gwtproject.rpc.serialization.api.emul" + packageName;
        }
        return packageName;
    }


    //custom field serializer may specify a looser type
    public Optional<TypeName> getDeserializeMethodParamType() {
        if (customFieldSerializer == null) {
            return Optional.of(getTypeName());
        }
        ExecutableElement method = CustomFieldSerializerValidator.getDeserializationMethod(types.getTypes(), customFieldSerializer, type);
        if (method == null) {
            return Optional.empty();//...hmm. GWT handles this by just emitting empty classes, which seems silly. But in this case,
            //            we know that there is a custom field serializer, which doesn't have the methods we need, so
            //            validation already failed and we aren't going to even try to consider this type as
            //            instantiable. I think.
        }
        return Optional.of(ClassName.get(types.getTypes().erasure(method.getParameters().get(1).asType())));
    }
    //custom field serializer may specify a looser type
    public Optional<TypeName> getSerializeMethodParamType() {
        if (customFieldSerializer == null) {
            return Optional.of(getTypeName());
        }
        ExecutableElement method = CustomFieldSerializerValidator.getSerializationMethod(types.getTypes(), customFieldSerializer, type);
        if (method == null) {
            return Optional.empty();//...hmm. GWT handles this by just emitting empty classes, which seems silly. But in this case,
            //            we know that there is a custom field serializer, which doesn't have the methods we need, so
            //            validation already failed and we aren't going to even try to consider this type as
            //            instantiable. I think.
        }
        return Optional.of(ClassName.get(types.getTypes().erasure(method.getParameters().get(1).asType())));
    }



}

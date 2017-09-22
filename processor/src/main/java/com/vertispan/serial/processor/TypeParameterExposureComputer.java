package com.vertispan.serial.processor;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.ClassName;
import com.vertispan.gwtapt.JTypeUtils;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * This class is used to compute type parameter exposure using a flow algorithm.
 */
class TypeParameterExposureComputer {

    /**
     * Helper class for type parameter flow information.
     */
    class TypeParameterFlowInfo {
        /**
         * The class that declares this type parameter.
         */
        private final DeclaredType baseType;

        /**
         * The keys are the set of type parameters that, if exposed, cause this type
         * parameter to be exposed. The value for each key is the dimensionality
         * that the exposure will cause. If the key is exposed as an array, then the
         * dimensionality should be added to the dimensionality that the key is
         * already exposed as.
         */
        private final Map<TypeParameterFlowInfo, Integer> causesExposure =
                new LinkedHashMap<TypeParameterFlowInfo, Integer>();

        private int exposure = EXPOSURE_NONE;

        private final Map<TypeParameterFlowInfo, Boolean> isTransitivelyAffectedByCache =
                new HashMap<TypeParameterFlowInfo, Boolean>();

        /**
         * Type parameters that need to be notified when my exposure changes.
         */
        private final Set<TypeParameterFlowInfo> listeners = new LinkedHashSet<TypeParameterFlowInfo>();

        private boolean mightNotBeExposed = true;

        /**
         * Ordinal of this type parameter.
         */
        private final int ordinal;

        private boolean visited;

        TypeParameterFlowInfo(DeclaredType baseType, int ordinal) {
            this.baseType = baseType;
            this.ordinal = ordinal;
        }

        public boolean checkDirectExposure() {
            boolean didChange = false;
            DeclaredType type = baseType;
            while (type != null) {
                // any problems should already have been captured by our caller, so we
                // make a throw-away ProblemReport here.
                if (SerializableTypeOracleBuilder.shouldConsiderFieldsForSerialization(types, type, typeFilter,
                        new ProblemReport(messager))) {
                    List<VariableElement> fields = ElementFilter.fieldsIn(types.getTypes().asElement(type).getEnclosedElements());
                    for (VariableElement field : fields) {
                        if (!SerializableTypeOracleBuilder.shouldConsiderForSerialization(field)) {
                            continue;
                        }

                        if (types.getTypes().isSameType(JTypeUtils.getLeafType(field.asType()), getTypeParameter())) {
              /*
               * If the type parameter is referenced explicitly or as the leaf
               * type of an array, then it will be considered directly exposed.
               */
                            markExposedAsArray(0);
                            mightNotBeExposed = false;
                            didChange = true;

                            if (field.asType().getKind() == TypeKind.ARRAY) {
                                markExposedAsArray(JTypeUtils.getRank(field.asType()));
                            }
                        }
                    }
                }

        /*
         * Counting on substitution to propagate the type parameter.
         */
                type = MoreTypes.nonObjectSuperclass(types.getTypes(), types.getElements(), type).orNull();
            }

            return didChange;
        }

        public Map<TypeParameterFlowInfo, Integer> getCausesExposure() {
            return causesExposure;
        }

        public int getExposure() {
            return exposure;
        }

        public Set<TypeParameterFlowInfo> getListeners() {
            return listeners;
        }

        /**
         * Return whether it is possible for the parameter not to be exposed. For
         * example, if a class has one subclass that uses the parameter and another
         * that does not, then the parameter is exposed (exposure >=
         * <code>EXPOSURE_DIRECT</code>) but this method will return
         * <code>false</code>.
         */
        public boolean getMightNotBeExposed() {
            return mightNotBeExposed;
        }

        /**
         * Determine whether there is an infinite array exposure if this type
         * parameter is used in an array type which is then passed as an actual type
         * argument for the formal type parameter <code>other</code>.
         */
        public boolean infiniteArrayExpansionPathBetween(TypeParameterFlowInfo other) {
            Integer dimensionDelta = getCausesExposure().get(other);
            if (dimensionDelta == null) {
                return false;
            }
            return dimensionDelta > 0 && other.isTransitivelyAffectedBy(this);
        }

        @Override
        public String toString() {
            return ClassName.get(getTypeParameter()) + " in " + ClassName.get(baseType);
        }

        public boolean updateFlowInfo() {
            boolean didChange = false;
            if (!wasVisited()) {
                didChange |= initializeExposure();
                markVisited();
            }

            for (Entry<TypeParameterFlowInfo, Integer> entry : getCausesExposure().entrySet()) {
                TypeParameterFlowInfo info2 = entry.getKey();
                int dimensionDelta = entry.getValue();
                if (info2.getExposure() >= 0) {
                    if (!infiniteArrayExpansionPathBetween(info2)) {
                        didChange |= markExposedAsArray(dimensionDelta + info2.getExposure());
                    }
                }
            }

            return didChange;
        }

        void addListener(TypeParameterFlowInfo listener) {
            listeners.add(listener);
        }

        TypeVariable getTypeParameter() {
            return (TypeVariable) baseType.getTypeArguments().get(ordinal);
        }

        boolean initializeExposure() {
            computeIndirectExposureCauses();
            return checkDirectExposure();
        }

        boolean isTransitivelyAffectedBy(TypeParameterFlowInfo flowInfo) {
            Boolean result = isTransitivelyAffectedByCache.get(flowInfo);
            if (result != null) {
                return result;
            }

            HashSet<TypeParameterFlowInfo> affectedBy = new HashSet<TypeParameterFlowInfo>();
            Set<TypeParameterFlowInfo> affectedByWorklist = new LinkedHashSet<TypeParameterFlowInfo>();
            affectedByWorklist.add(this);

            result = false;
            while (!affectedByWorklist.isEmpty()) {
                TypeParameterFlowInfo currFlowInfo = affectedByWorklist.iterator().next();
                affectedByWorklist.remove(currFlowInfo);

                if (currFlowInfo == flowInfo) {
                    result = true;
                    break;
                }

                if (affectedBy.add(currFlowInfo)) {
                    affectedByWorklist.addAll(currFlowInfo.getAffectedBy());
                }
            }

            isTransitivelyAffectedByCache.put(flowInfo, result);
            return result;
        }

        boolean markExposedAsArray(int dim) {
            if (exposure >= dim) {
                return false;
            }

            exposure = dim;
            return true;
        }

        void markVisited() {
            visited = true;
        }

        boolean wasVisited() {
            return visited;
        }

        private void computeIndirectExposureCauses() {
            // TODO(spoon): this only needs to consider immediate subtypes, not all
            // subtypes
            List<TypeMirror> subtypes = types.getSubtypes(baseType).stream().map(Element::asType).collect(Collectors.toList());//TODO stick bit. where to get all possible types that we might need to reach...
            for (TypeMirror subtype : subtypes) {
                if (!(subtype instanceof DeclaredType)) {
                    // Only generic types can cause a type parameter to be exposed
                    continue;
                }
                DeclaredType isGeneric = (DeclaredType) subtype;

                // any problems should already have been captured by our caller, so we
                // make a throw-away ProblemReport here.
                if (!SerializableTypeOracleBuilder.shouldConsiderFieldsForSerialization(types, subtype,
                        typeFilter, new ProblemReport(messager))) {
                    continue;
                }

                DeclaredType asParameterizationOf = JTypeUtils.asParameterizationOf(types.getTypes(), subtype, baseType);
                Set<TypeVariable> paramsUsed = new LinkedHashSet<>();
                SerializableTypeOracleBuilder.recordTypeParametersIn(
                        asParameterizationOf.getTypeArguments().get(ordinal), paramsUsed);

                int ordinal = 0;
                for (TypeVariable paramUsed : paramsUsed) {
                    recordCausesExposure(isGeneric, ordinal++, 0);
                }
            }

            TypeMirror type = baseType;
            while (type != null) {
                if (SerializableTypeOracleBuilder.shouldConsiderFieldsForSerialization(types, type, typeFilter,
                        new ProblemReport(messager))) {
                    List<VariableElement> fields = ElementFilter.fieldsIn(types.getTypes().asElement(type).getEnclosedElements());
                    for (VariableElement field : fields) {
                        if (!SerializableTypeOracleBuilder.shouldConsiderForSerialization(field)) {
                            continue;
                        }

                        TypeMirror fieldType = JTypeUtils.getLeafType(field.asType());
                        if (!(fieldType instanceof DeclaredType)) {
                            continue;
                        }
                        DeclaredType isParameterized = (DeclaredType) fieldType;

                        List<? extends TypeMirror> typeArgs = isParameterized.getTypeArguments();
                        for (int i = 0; i < typeArgs.size(); ++i) {
                            if (referencesTypeParameter(typeArgs.get(i), getTypeParameter())) {
                                DeclaredType genericFieldType = (DeclaredType) isParameterized.asElement().asType();
                                recordCausesExposure(genericFieldType, i, 0);
                                if (typeArgs.get(i).getKind() == TypeKind.ARRAY) {
                                    ArrayType arrayType = (ArrayType) typeArgs.get(i);
                                    if (types.getTypes().isSameType(JTypeUtils.getLeafType(arrayType), getTypeParameter())) {
                                        recordCausesExposure(genericFieldType, i, JTypeUtils.getRank(arrayType));
                                    }
                                }
                            }
                        }
                    }
                }

        /*
         * Counting on substitution to propagate the type parameter.
         */
                type = MoreTypes.nonObjectSuperclass(types.getTypes(), types.getElements(), (DeclaredType) type).orNull();
            }
        }

        private Collection<? extends TypeParameterFlowInfo> getAffectedBy() {
            return causesExposure.keySet();
        }

        /**
         * The same as
         * {@link TypeParameterExposureComputer#getFlowInfo(JGenericType, int)},
         * except that it additionally adds <code>this</code> as a listener to the
         * returned flow info.
         */
        private TypeParameterFlowInfo getFlowInfo(DeclaredType type, int index) {
            TypeParameterFlowInfo flowInfo = TypeParameterExposureComputer.this.getFlowInfo(type, index);
            flowInfo.addListener(this);
            return flowInfo;
        }

        private void recordCausesExposure(DeclaredType type, int index, int level) {
            assert (index < type.getTypeArguments().size());
            TypeParameterFlowInfo flowInfo = getFlowInfo(type, index);
            Integer oldLevel = causesExposure.get(flowInfo);
            if (oldLevel == null || oldLevel < level) {
                causesExposure.put(flowInfo, level);
            }
        }

        private boolean referencesTypeParameter(TypeMirror classType, TypeVariable typeParameter) {
            Set<TypeVariable> typeParameters = new LinkedHashSet<>();
            SerializableTypeOracleBuilder.recordTypeParametersIn(classType, typeParameters);
            return typeParameters.contains(typeParameter);
        }
    }

    /**
     * Type parameter is exposed.
     */
    static final int EXPOSURE_DIRECT = 0;

    /**
     * Type parameter is exposed as a bounded array. The value is the max bound of
     * the exposure.
     */
    static final int EXPOSURE_MIN_BOUNDED_ARRAY = EXPOSURE_DIRECT + 1;

    /**
     * Type parameter is not exposed.
     */
    static final int EXPOSURE_NONE = -1;

    private final SerializingTypes types;

    private TypeFilter typeFilter;

    private final Map<TypeMirror, TypeParameterFlowInfo> typeParameterToFlowInfo =
            new IdentityHashMap<>();

    private final Set<TypeParameterFlowInfo> worklist = new LinkedHashSet<TypeParameterFlowInfo>();

    private final Messager messager;

    TypeParameterExposureComputer(SerializingTypes types, TypeFilter typeFilter, Messager messager) {
        this.types = types;
        this.typeFilter = typeFilter;
        this.messager = messager;
    }

    /**
     * Computes flow information for the specified type parameter. If it has
     * already been computed just return the value of the previous computation.
     *
     * @param type the generic type whose type parameter flow we are interested in
     * @param index the index of the type parameter whose flow we want to compute
     */
    public TypeParameterFlowInfo computeTypeParameterExposure(DeclaredType type, int index) {
        // check if it has already been computed
        List<? extends TypeMirror> typeParameters = type.getTypeArguments();
        assert (index < typeParameters.size());
        TypeVariable typeParameter = (TypeVariable) typeParameters.get(index);
        TypeParameterFlowInfo queryFlow = typeParameterToFlowInfo.get(typeParameter);
        if (queryFlow != null) {
            return queryFlow;
        }

        // not already computed; compute it
        queryFlow = getFlowInfo(type, index); // adds it to the work list as a
        // side effect

        while (!worklist.isEmpty()) {
            TypeParameterFlowInfo info = worklist.iterator().next();
            worklist.remove(info);

            boolean didChange = info.updateFlowInfo();

            if (didChange) {
                for (TypeParameterFlowInfo listener : info.getListeners()) {
                    worklist.add(listener);
                }
            }
        }

        return queryFlow;
    }

    public void setTypeFilter(TypeFilter typeFilter) {
        this.typeFilter = typeFilter;
    }

    /**
     * Return the parameter flow info for a type parameter specified by class and
     * index. If the flow info did not previously exist, create it and add it to
     * the work list.
     */
    private TypeParameterFlowInfo getFlowInfo(DeclaredType type, int index) {
        TypeMirror typeParameter = type.getTypeArguments().get(index);
        TypeParameterFlowInfo info = typeParameterToFlowInfo.get(typeParameter);
        if (info == null) {
            info = new TypeParameterFlowInfo(type, index);
            typeParameterToFlowInfo.put(typeParameter, info);
            worklist.add(info);
        }
        return info;
    }
}

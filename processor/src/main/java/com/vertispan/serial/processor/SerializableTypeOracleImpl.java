package com.vertispan.serial.processor;

import javax.lang.model.type.TypeMirror;
import java.util.Set;

final class SerializableTypeOracleImpl implements SerializableTypeOracle {

    private final Set<TypeMirror> possiblyInstantiatedTypes;
    private final Set<TypeMirror> serializableTypesSet;

    public SerializableTypeOracleImpl(Set<TypeMirror> serializableTypes,
                                      Set<TypeMirror> possiblyInstantiatedTypes) {

        serializableTypesSet = serializableTypes;
        this.possiblyInstantiatedTypes = possiblyInstantiatedTypes;
    }

    public TypeMirror[] getSerializableTypes() {
        return serializableTypesSet.toArray(new TypeMirror[serializableTypesSet.size()]);
    }

    /**
     * Returns <code>true</code> if the type's fields can be serializede.
     */
    public boolean isSerializable(TypeMirror type) {
        return serializableTypesSet.contains(type);
    }

    /**
     * Returns <code>true</code> if the type can be serialized and then
     * instantiated on the other side.
     */
    public boolean maybeInstantiated(TypeMirror type) {
        return possiblyInstantiatedTypes.contains(type);
    }
}

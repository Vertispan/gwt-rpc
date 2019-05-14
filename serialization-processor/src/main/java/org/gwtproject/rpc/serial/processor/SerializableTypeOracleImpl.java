package org.gwtproject.rpc.serial.processor;

import com.squareup.javapoet.TypeName;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Set;

final class SerializableTypeOracleImpl implements SerializableTypeOracle {

    private final Set<TypeMirror> possiblyInstantiatedTypes;
    private final Set<TypeMirror> serializableTypesSet;
    private final Types types;

    public SerializableTypeOracleImpl(Set<TypeMirror> serializableTypes,
                                      Set<TypeMirror> possiblyInstantiatedTypes, Types types) {

        serializableTypesSet = serializableTypes;
        this.possiblyInstantiatedTypes = possiblyInstantiatedTypes;
        this.types = types;
    }

    public TypeMirror[] getSerializableTypes() {
        return serializableTypesSet.toArray(new TypeMirror[serializableTypesSet.size()]);
    }

    /**
     * Returns <code>true</code> if the type's fields can be serialized.
     */
    public boolean isSerializable(TypeMirror type) {
        String typeName = TypeName.get(types.erasure(type)).toString();
        return serializableTypesSet.stream().anyMatch(candidate -> TypeName.get(candidate).toString().equals(typeName));
    }

    /**
     * Returns <code>true</code> if the type can be serialized and then
     * instantiated on the other side.
     */
    public boolean maybeInstantiated(TypeMirror type) {
        String typeName = TypeName.get(types.erasure(type)).toString();
        return possiblyInstantiatedTypes.stream().anyMatch(candidate -> TypeName.get(candidate).toString().equals(typeName));
    }
}

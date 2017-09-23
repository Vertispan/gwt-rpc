package com.vertispan.serial.processor;

import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class SerializableTypeOracleUnion implements SerializableTypeOracle {
    private final Set<SerializableTypeOracle> subOracles;

    public SerializableTypeOracleUnion(Set<SerializableTypeOracle> subOracles) {
        this.subOracles = Collections.unmodifiableSet(subOracles);
    }

    @Override
    public TypeMirror[] getSerializableTypes() {
        return subOracles.stream().flatMap(oracle -> Stream.of(oracle.getSerializableTypes())).distinct().toArray(TypeMirror[]::new);
    }

    @Override
    public boolean isSerializable(TypeMirror type) {
        return subOracles.stream().anyMatch(oracle -> oracle.isSerializable(type));
    }

    @Override
    public boolean maybeInstantiated(TypeMirror type) {
        return subOracles.stream().anyMatch(oracle -> oracle.maybeInstantiated(type));
    }
}

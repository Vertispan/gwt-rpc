package org.gwtproject.rpc.serial.processor;

import javax.lang.model.type.TypeMirror;
import java.util.Set;

/**
 * Interface implemented by any class that wants to answer questions about
 * serializable types for a given type serializer.
 */
public interface SerializableTypeOracle {

    /**
     * Returns the list of all types that are considered serializable.
     *
     * @return array of serializable types
     */
    Set<TypeMirror> getSerializableTypes();

    /**
     * Returns true if the type is serializable. If a type is serializable then
     * there is a secondary type called a FieldSerializer that provides the
     * behavior necessary to serialize or deserialize the fields of an instance.
     *
     * @param type the type that maybe serializable
     * @return true if the type is serializable
     */
    boolean isSerializable(TypeMirror type);

    /**
     * Returns <code>true</code> if the type might be instantiated as part of
     * deserialization or serialization.
     *
     * @param type the type to test
     * @return <code>true</code> if the type might be instantiated
     */
    boolean maybeInstantiated(TypeMirror type);
}

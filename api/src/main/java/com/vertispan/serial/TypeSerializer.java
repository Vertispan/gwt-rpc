package com.vertispan.serial;

/**
 * Contract for any class that can serialize and restore class into a
 * serialization stream.
 */
public interface TypeSerializer {

    /**
     * Restore an instantiated object from the serialized stream.
     */
    void deserialize(SerializationStreamReader stream, Object instance,
                     String typeSignature) throws SerializationException;

    /**
     * Return the serialization signature for the given type.
     */
    String getSerializationSignature(Class<?> clazz);

    /**
     * Instantiate an object of the given typeName from the serialized stream.
     */
    Object instantiate(SerializationStreamReader stream, String typeSignature)
            throws SerializationException;

    /**
     * Save an instance into the serialization stream.
     */
    void serialize(SerializationStreamWriter stream, Object instance,
                   String typeSignature) throws SerializationException;
}
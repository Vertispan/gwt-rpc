package org.gwtproject.rpc.serialization.api;

/**
 * Do not use this for CustomFieldSerializers. Instead, write only public static methods, which will be validated
 * at compile time. Non-public methods will be ignored during validation. Allowed/supported methods:
 *   o MyObject instantiate(SerializationStreamReader reader) throws SerializationException
 *   o void read(MyObject instance, SerializationStreamReader reader) throws SerializationException
 *   o void write(MyObject instance, SerializationStreamWriter writer) throws SerializationException
 *
 * These will be called automatically by the generated field serializer, in addition to any required superclass
 * serialization/deserialization.
 */
public interface FieldSerializer {
    default void deserial(SerializationStreamReader reader, Object instance) throws
            SerializationException, com.google.gwt.user.client.rpc.SerializationException {
        throw new IllegalStateException("Cannot deserialize instance of this type, as it was not marked to be read. " + toString());
    }
    default void serial(SerializationStreamWriter writer, Object instance) throws SerializationException, com.google.gwt.user.client.rpc.SerializationException {
        throw new IllegalStateException("Cannot serialize instance of this type, as it was not marked to be written. " + toString());
    }
    default Object create(SerializationStreamReader reader) throws SerializationException, com.google.gwt.user.client.rpc.SerializationException {
        //default implementation throws an exception to indicate that deserialization isn't possible
        throw new IllegalStateException("Cannot create an instance of this type. Possible reasons: was not marked to be read, is abstract, has no default constructor, or only subtypes are whitelisted. " + toString());
    }
}

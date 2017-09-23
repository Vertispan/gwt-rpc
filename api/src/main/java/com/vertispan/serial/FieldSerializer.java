package com.vertispan.serial;

public interface FieldSerializer {
    default <T> void deserial(SerializationStreamReader reader, T instance) throws
            SerializationException {

    }
    default <T> void serial(SerializationStreamWriter writer, T instance) throws SerializationException {

    }
    default Object create(SerializationStreamReader reader) throws SerializationException {
        throw new IllegalStateException("Cannot create an instance of this type - abstract, has no default constructor, or only subtypes are whitelisted");
    }
}

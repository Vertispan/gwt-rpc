package org.gwtproject.rpc.serialization.api.impl;

import org.gwtproject.rpc.serialization.api.*;

/**
 * Trivial implementation, assuming that the actual serializers are mapped by Class.getName.
 */
public abstract class TypeSerializerImpl implements TypeSerializer {
    protected abstract FieldSerializer serializer(String name);

    @Override
    public void deserialize(SerializationStreamReader stream, Object instance, String typeSignature) throws SerializationException, com.google.gwt.user.client.rpc.SerializationException {
        serializer(typeSignature).deserial(stream, instance);
    }

    @Override
    public String getSerializationSignature(Class<?> clazz) {
        return clazz.getName();
    }

    @Override
    public Object instantiate(SerializationStreamReader stream, String typeSignature) throws SerializationException, com.google.gwt.user.client.rpc.SerializationException {
        return serializer(typeSignature).create(stream);
    }

    @Override
    public void serialize(SerializationStreamWriter stream, Object instance, String typeSignature) throws SerializationException, com.google.gwt.user.client.rpc.SerializationException {
        serializer(typeSignature).serial(stream, instance);
    }
}

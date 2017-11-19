package com.vertispan.serial.impl;

import com.vertispan.serial.*;

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

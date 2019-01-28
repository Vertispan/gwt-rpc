package com.google.gwt.user.client.rpc;

@Deprecated
public abstract class CustomFieldSerializer<T> {

    public abstract void deserializeInstance(
            SerializationStreamReader streamReader, T instance)
            throws SerializationException;

    public boolean hasCustomInstantiateInstance() {
        return false;
    }

    public T instantiateInstance(SerializationStreamReader streamReader)
            throws SerializationException {
        throw new SerializationException(
                "instantiateInstance is not supported by " + getClass().getName());
    }

    public abstract void serializeInstance(SerializationStreamWriter streamWriter,
                                           T instance) throws SerializationException;
}

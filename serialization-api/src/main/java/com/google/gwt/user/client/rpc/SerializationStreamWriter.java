package com.google.gwt.user.client.rpc;

@Deprecated
public interface SerializationStreamWriter {
    void writeBoolean(boolean value) throws SerializationException;

    void writeByte(byte value) throws SerializationException;

    void writeChar(char value) throws SerializationException;

    void writeDouble(double value) throws SerializationException;

    void writeFloat(float value) throws SerializationException;

    void writeInt(int value) throws SerializationException;

    void writeLong(long value) throws SerializationException;

    void writeObject(Object value) throws SerializationException;

    void writeShort(short value) throws SerializationException;

    void writeString(String value) throws SerializationException;
}


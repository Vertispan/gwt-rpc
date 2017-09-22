package com.vertispan.serial;

public interface SerializationStreamWriter {
    String toString();

    void writeBoolean(boolean var1) throws SerializationException;

    void writeByte(byte var1) throws SerializationException;

    void writeChar(char var1) throws SerializationException;

    void writeDouble(double var1) throws SerializationException;

    void writeFloat(float var1) throws SerializationException;

    void writeInt(int var1) throws SerializationException;

    void writeLong(long var1) throws SerializationException;

    void writeObject(Object var1) throws SerializationException;

    void writeShort(short var1) throws SerializationException;

    void writeString(String var1) throws SerializationException;
}

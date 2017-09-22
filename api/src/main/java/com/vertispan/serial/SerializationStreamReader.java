package com.vertispan.serial;

/**
 * An interface for reading values from a stream.
 */
public interface SerializationStreamReader {

    boolean readBoolean() throws SerializationException;

    byte readByte() throws SerializationException;

    char readChar() throws SerializationException;

    double readDouble() throws SerializationException;

    float readFloat() throws SerializationException;

    int readInt() throws SerializationException;

    long readLong() throws SerializationException;

    Object readObject() throws SerializationException;

    short readShort() throws SerializationException;

    String readString() throws SerializationException;

}

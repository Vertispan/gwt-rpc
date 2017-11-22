package com.vertispan.serial;

/**
 * Interface describing how values can be written to a stream. Implementations decide
 * how to wrap {@link TypeSerializer} and how to provide the finished, serialized data
 * to be sent over the wire.
 *
 * @see SerializationStreamReader
 */
public interface SerializationStreamWriter extends com.google.gwt.user.client.rpc.SerializationStreamWriter {

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeBoolean(boolean value) throws SerializationException;

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeByte(byte value) throws SerializationException;

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeChar(char value) throws SerializationException;

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeDouble(double value) throws SerializationException;

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeFloat(float value) throws SerializationException;

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeInt(int value) throws SerializationException;

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeLong(long value) throws SerializationException;

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeObject(Object value) throws SerializationException, com.google.gwt.user.client.rpc.SerializationException;

    /**
     *
     * @param value
     * @throws SerializationException
     */
    void writeShort(short value) throws SerializationException;

    /**
     * 
     * @param value
     * @throws SerializationException
     */
    void writeString(String value) throws SerializationException;
}


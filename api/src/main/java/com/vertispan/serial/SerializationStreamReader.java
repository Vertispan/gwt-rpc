package com.vertispan.serial;

/**
 * An interface for reading values from a stream. Many of the aspects of how this operates are
 * implementation specific, such as when a {@link SerializationException} might be thrown, or
 * how "slots" in {@link #claimItems(int)} are decided. Implementations decide how to wrap a
 * {@link TypeSerializer} and the serialized data, usually in the constructor.
 *
 * Some assumptions are made about the underlying stream
 *  - within the same stream, if the same object is written twice, both calls to
 *    {@link #readObject()} will return the same instance.
 *  - the same applies to Strings, which are expected to be stored in a string table. Reading
 *    a string either acts like it is an object, or just directly can call {@link #readString()}.
 *  - Strings are indexed separately from objects in their own table, allowing them  to be passed
 *    separately, and avoid treating them like objects when possible (with an additional entry in
 *    the payload)
 * The base implementation of this interface, AbstractSerializationStreamReader, further assumes
 * that a new object consists of a positive int, that positive int references a string
 *
 * @see SerializationStreamWriter
 */
public interface SerializationStreamReader {

    /**
     * Reads the next piece of data in the stream as if it were a boolean.
     * @return true or false
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    boolean readBoolean() throws SerializationException;

    /**
     * Rads the next piece of data in the stream as if it were a byte.
     * @return the next byte in the stream
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    byte readByte() throws SerializationException;

    /**
     * Reads the next piece of data in the stream as if it were a char.
     * @return the next char in the stream
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    char readChar() throws SerializationException;

    /**
     * Reads the next piece of data in the stream as if it were a double.
     * @return the next double in the stream
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    double readDouble() throws SerializationException;

    /**
     * Reads the next piece of data in the stream as if it were a float.
     * @return the next float in the stream
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    float readFloat() throws SerializationException;

    /**
     * Reads the next piece of data in the stream as if it were a int.
     * @return the next int in the stream
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    int readInt() throws SerializationException;

    /**
     * Reads the next piece of data in the stream as if it were a long.
     * @return the next long in the stream
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    long readLong() throws SerializationException;

    /**
     * Reads the next piece of data in the stream as if it were an Object, delegating to
     * the wrapped TypeSerializer to instantiate instances and deserialize fields. Will
     * be recursive as necessary to pick up more objects and fields.
     * @return the next Object in the stream, with all of its fields
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    Object readObject() throws SerializationException;

    /**
     * Reads the next piece of data in the stream as if it were a short.
     * @return the next short in the stream
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    short readShort() throws SerializationException;

    /**
     * Reads the next reference from the stream, and use that to look up a string in the table.
     * @return the next String in the stream, without reading it as an object first
     * @throws SerializationException if not enough data remains, or the data is not formatted correctly
     */
    String readString() throws SerializationException;

    /**
     * Utility to ensure that there are enough remaining pieces of data in the payload
     * to allow an array of that size to be allocated. Normally this will just do a quick
     * bounds check which will pass, and decrement a field in the stream to prevent those
     * items from being claimed again.
     *
     * The attack that this mitigates is where a client pretends to have an Object[]
     * with 1m items, and the first item in there is an Object[] with 1m items, and
     * so on. Each "layer" deep needs enough memory for another million references so with
     * a request that is only a few kb, many gigabytes of memory are needed.
     *
     * "Claiming" those slots ahead of time would force a bounds check on the total size
     * of the incoming payload - if there aren't a million items in the payload, obviously
     * that array won't actually be that large. Once they are claimed by one array, they
     * can't be claimed again, so even if there are 1 million items, the next array can't
     * also pretend to be that large, unless the incoming stream is actually that big.
     *
     * As an implementation detail, the range check should be performed such that the
     * sum of all claimed items is smaller than the size of the smallest item, across
     * all parts of the stream (main payload and string table). For example, if a stream
     * implementation stores booleans as a single bit, but longs as 64 bits, a boolean[]
     * could well be 64 times smaller than a long[], but this limits the attacker to only
     * consuming a constant factor more memory than the request itself uses. The check
     * might look something like this:
     * 
     *   if (alreadyClaimed + newlyClaimed > slotsInStream + stringsInTable) {
     *       throw new SerializationException("Request claims to be larger than it is");
     *   }
     *   alreadyClaimed += newlyClaimed;
     * @param slots the number of items in the collection being deserialized
     * @throws SerializationException if there are too few remaining pieces of data in the
     *                                stream to allow this collection to be deserialized
     */
    void claimItems(int slots) throws SerializationException;

}

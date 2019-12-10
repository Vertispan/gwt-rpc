package org.gwtproject.rpc.serialization.stream.bytebuffer;

import org.gwtproject.rpc.serialization.api.SerializationException;
import org.gwtproject.rpc.serialization.api.TypeSerializer;
import org.gwtproject.rpc.serialization.api.impl.AbstractSerializationStreamWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * Simple ByteBuffer-based serialization stream writer, which encodes the payload in a bytebuffer,
 * but stores strings in a string table. Subclasses might write all streams and the payload into
 * a single bytebuffer, suitable for sending over the wire or compressing, while others might keep
 * the data separate to avoid an extra copy, such as communicating between browser workers.
 */
public class ByteBufferSerializationStreamWriter  extends AbstractSerializationStreamWriter {
    private ByteBuffer bb;//initial size 1kb

    private final TypeSerializer serializer;

    public ByteBufferSerializationStreamWriter(TypeSerializer serializer) {
        this.serializer = serializer;
        bb = ByteBuffer.allocate(1024);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.position(3 << 2);
    }

    /**
     * Gets the bytes for the stream. Can only be called once, will prevent more
     * data from being written.
     */
    public ByteBuffer getPayloadBytes() {
        Objects.requireNonNull(bb);

        bb.limit(bb.position());
        bb.position(0);
        bb = bb.slice();//http://thecodelesscode.com/case/209

        bb.order(ByteOrder.LITTLE_ENDIAN);

        bb.putInt(0 << 2, getVersion());
        bb.putInt(1 << 2, getFlags());
        //mark the size of the payload
        bb.putInt(2 << 2, bb.limit() - (3 << 2));

        ByteBuffer retVal = bb;
        bb = null;
        return retVal;
    }

    public String[] getFinishedStringTable() {
        List<String> stringTable = getStringTable();
        return stringTable.toArray(new String[0]);
    }

    public ByteBuffer getFullPayload() {
        ByteBuffer payloadBytes = getPayloadBytes();
        List<String> stringTable = getStringTable();
        int stringCount = stringTable.size();
        if (stringCount == 0) {
            return payloadBytes;
        }

        // make a buffer big enough to store all strings (and lengths)

        // start with the size of the regular data, one int per string, and one int to count the strings
        int size = payloadBytes.limit() + ((1 + stringCount) << 2);

        // add the length of each string in bytes, and store the converted strings so we don't need to do it again
        byte[][] stringBytes = new byte[stringCount][];
        for (int i = 0; i < stringCount; i++) {
            byte[] bytes = stringTable.get(i).getBytes(Charset.forName("UTF-8"));
            stringBytes[i] = bytes;
            size += bytes.length;
        }
        ByteBuffer bb = ByteBuffer.allocate(size);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        // append the payload, the count of strings, and the strings themselves
        bb.put(payloadBytes);
        bb.putInt(stringCount);
        for (int i = 0; i < stringCount; i++) {
            byte[] bytes = stringBytes[i];
            bb.putInt(bytes.length);
            bb.put(bytes);
        }

        bb.position(0);

        return bb;
    }


    @Override
    public String toString() {
        return "StreamWriter";
    }

    @Override
    public void writeLong(long l) {
        maybeGrow();
        bb.putLong(l);
    }

    public void writeBoolean(boolean fieldValue) {
        maybeGrow();
        bb.put((byte) (fieldValue ? 1 : 0));
    }

    @Override
    public void writeByte(byte fieldValue) {
        maybeGrow();
        bb.put(fieldValue);
    }

    @Override
    public void writeChar(char ch) {
        maybeGrow();
        bb.putChar(ch);
    }

    @Override
    public void writeFloat(float fieldValue) {
        maybeGrow();
        bb.putFloat(fieldValue);
    }

    @Override
    public void writeDouble(double fieldValue) {
        maybeGrow();
        bb.putDouble(fieldValue);
    }

    @Override
    public void writeInt(int fieldValue) {
        maybeGrow();
        bb.putInt(fieldValue);
    }

    private void maybeGrow() {
        if (bb.remaining() < 8) {//always want at least 8 bytes remaining for doubles or longs
            ByteBuffer old = bb;
            bb = ByteBuffer.allocate(old.capacity() * 2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put((ByteBuffer) old.flip());
        }
    }

    @Override
    public void writeShort(short value) {
        maybeGrow();
        bb.putShort(value);
    }

    @Override
    protected void append(String s) {
        //strangely enough, we do nothing, and wait until we actually are asked to write the whole thing out
    }

    @Override
    protected String getObjectTypeSignature(Object o) throws SerializationException {
        Class clazz = o.getClass();
        if(o instanceof Enum) {
            Enum e = (Enum)o;
            clazz = e.getDeclaringClass();
        }

        return this.serializer.getSerializationSignature(clazz);
    }

    @Override
    protected void serialize(Object o, String s) throws com.google.gwt.user.client.rpc.SerializationException {
        this.serializer.serialize(this, o, s);
    }
}


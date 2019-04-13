package org.gwtproject.rpc.serialization.stream.bytebuffer;

import org.gwtproject.rpc.serialization.api.SerializationException;
import org.gwtproject.rpc.serialization.api.TypeSerializer;
import org.gwtproject.rpc.serialization.api.impl.AbstractSerializationStreamWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
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
    //constants from BigLongLibBase for long->int[] conversion
    protected static final int BITS = 22;
    protected static final int BITS01 = 2 * BITS;
    protected static final int BITS2 = 64 - BITS01;
    protected static final int MASK = (1 << BITS) - 1;
    protected static final int MASK_2 = (1 << BITS2) - 1;

    private ByteBuffer bb;//initial size 1kb
    private IntBuffer payload;//an int32 view on bb

    private final TypeSerializer serializer;


    public ByteBufferSerializationStreamWriter(TypeSerializer serializer) {
        this.serializer = serializer;
        bb = ByteBuffer.allocate(1024);
        bb.order(ByteOrder.nativeOrder());
        payload = bb.asIntBuffer();
        payload.position(3);//leave room for flags, version, size
    }

    /**
     * Gets the bytes for the stream. Can only be called once, will prevent more
     * data from being written.
     */
    public ByteBuffer getPayloadBytes() {
        Objects.requireNonNull(bb);

        payload.limit(payload.position());
        payload.position(0);
        payload = payload.slice();

        payload.put(0, getVersion());
        payload.put(1, getFlags());
        //mark the size of the payload
        payload.put(2, payload.limit() - 3);

        bb.limit(payload.limit() << 2);
        bb.position(0);
        bb = bb.slice();//http://thecodelesscode.com/case/209
        bb.order(ByteOrder.nativeOrder());

        ByteBuffer retVal = bb;
        bb = null;
        payload = null;
        return retVal;
    }

    public String[] getFinishedStringTable() {
        List<String> stringTable = getStringTable();
        return stringTable.toArray(new String[stringTable.size()]);
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
        bb.order(ByteOrder.nativeOrder());

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
        //impl from BigLongLibBase
        int[] a = new int[3];
        a[0] = (int) (l & MASK);
        a[1] = (int) ((l >> BITS) & MASK);
        a[2] = (int) ((l >> BITS01) & MASK_2);
        writeInt(a[0]);
        writeInt(a[1]);
        writeInt(a[2]);
    }

    public void writeBoolean(boolean fieldValue) {
        writeInt(fieldValue ? 1 : 0);//wasteful, but no need to try to pack this way
    }

    @Override
    public void writeByte(byte fieldValue) {
        writeInt(fieldValue);//wasteful, but no need to try to pack this way
    }

    @Override
    public void writeChar(char ch) {
        writeInt(ch);
    }

    @Override
    public void writeFloat(float fieldValue) {
        maybeGrow();
        bb.asFloatBuffer().put(payload.position(), fieldValue);
        payload.position(payload.position() + 1);
    }

    @Override
    public void writeDouble(double fieldValue) {
        writeLong(Double.doubleToLongBits(fieldValue));
    }

    @Override
    public void writeInt(int fieldValue) {
        maybeGrow();
        payload.put(fieldValue);
    }

    private void maybeGrow() {
        if (!payload.hasRemaining()) {
            ByteBuffer old = bb;
            bb = ByteBuffer.allocate(old.capacity() * 2);
            bb.order(ByteOrder.nativeOrder());
            IntBuffer oldPayload = payload;
            payload = bb.asIntBuffer();
            payload.put((IntBuffer)oldPayload.flip());
        }
    }

    @Override
    public void writeShort(short value) {
        writeInt(value);
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


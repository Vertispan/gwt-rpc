package org.gwtproject.rpc.serialization.stream.bytebuffer;


import org.gwtproject.rpc.serialization.api.SerializationException;
import org.gwtproject.rpc.serialization.api.TypeSerializer;
import org.gwtproject.rpc.serialization.api.impl.AbstractSerializationStreamReader;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ByteBufferSerializationStreamReader extends AbstractSerializationStreamReader {
    private final TypeSerializer serializer;//b
    private final ByteBuffer bb;
    private final IntBuffer payload;
    private final String[] strings;

    private int claimedTokens;

    public ByteBufferSerializationStreamReader(TypeSerializer serializer, ByteBuffer bb, String[] strings) {
        this.serializer = serializer;
        this.bb = bb;
        this.payload = bb.asIntBuffer();
        this.strings = strings;
        int version = payload.get();
        int flags = payload.get();
        int length = payload.get();
//        assert length == payload.remaining();
        setVersion(version);
        setFlags(flags);
    }

    @Override
    protected Object deserialize(String s) throws com.google.gwt.user.client.rpc.SerializationException {
        int id = reserveDecodedObjectIndex();
        Object instance = serializer.instantiate(this, s);
        rememberDecodedObject(id, instance);
        serializer.deserialize(this, instance, s);
        return instance;
    }

    @Override
    protected String getString(int i) {
        return i > 0 ? strings[i - 1] : null;
    }

    @Override
    public boolean readBoolean() throws SerializationException {
        return payload.get() == 1;//or zero
    }

    @Override
    public byte readByte() throws SerializationException {
        return (byte) payload.get();
    }

    @Override
    public char readChar() throws SerializationException {
        return (char) payload.get();
    }

    @Override
    public double readDouble() throws SerializationException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public float readFloat() throws SerializationException {

        int position = payload.position();
        payload.position(position + 1);
        return bb.asFloatBuffer().get(position);
    }

    @Override
    public int readInt() throws SerializationException {
        return payload.get();
    }

    @Override
    public long readLong() throws SerializationException {
        int[] a = new int[3];
        a[0] = readInt();
        a[1] = readInt();
        a[2] = readInt();
        assert a[0] == (a[0] & ByteBufferSerializationStreamWriter.MASK);
        assert a[1] == (a[1] & ByteBufferSerializationStreamWriter.MASK);
        assert a[2] == (a[2] & ByteBufferSerializationStreamWriter.MASK_2);
        return (long) a[0] + ((long) a[1] << (long) ByteBufferSerializationStreamWriter.BITS) + ((long) a[2] << (long) ByteBufferSerializationStreamWriter.BITS01);
    }

    @Override
    public short readShort() throws SerializationException {
        return (short) payload.get();
    }

    @Override
    public String readString() throws SerializationException {
        return getString(readInt());
    }

    @Override
    public void claimItems(int slots) throws SerializationException {
        if (claimedTokens + slots > payload.remaining() + strings.length) {
            throw new SerializationException("Request claims to be larger than it is");
        }
        claimedTokens += slots;
    }
}

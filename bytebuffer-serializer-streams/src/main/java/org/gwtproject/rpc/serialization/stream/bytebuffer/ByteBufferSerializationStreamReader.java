package org.gwtproject.rpc.serialization.stream.bytebuffer;


import org.gwtproject.rpc.serialization.api.SerializationException;
import org.gwtproject.rpc.serialization.api.TypeSerializer;
import org.gwtproject.rpc.serialization.api.impl.AbstractSerializationStreamReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class ByteBufferSerializationStreamReader extends AbstractSerializationStreamReader {
    private final TypeSerializer serializer;//b
    private final ByteBuffer bb;
    private final IntBuffer payload;
    private final String[] strings;

    private int claimedTokens;

    public ByteBufferSerializationStreamReader(TypeSerializer serializer, ByteBuffer bb, String[] strings) {
        bb.order(ByteOrder.nativeOrder());

        this.serializer = serializer;
        this.bb = bb;
        this.payload = bb.asIntBuffer();
        this.strings = strings;
        int version = payload.get();
        int flags = payload.get();
        int length = payload.get();
        assert length == payload.remaining();
        setVersion(version);
        setFlags(flags);
    }

    public ByteBufferSerializationStreamReader(TypeSerializer serializer, ByteBuffer bb) {
        bb.order(ByteOrder.nativeOrder());

        this.serializer = serializer;
        this.bb = bb;
        this.payload = bb.asIntBuffer();
        int version = payload.get();
        int flags = payload.get();
        int length = payload.get();
        setVersion(version);
        setFlags(flags);

        //strings are in the payload, read them out first and assign them
        String[] strings = new String[0];
        // see if there is a stringCount, and thus strings present
        if (payload.limit() > 3 + length) {
            int stringsCount = payload.get(3 + length);
            if (stringsCount < 1) {
                throw new IllegalArgumentException("Invalid string count in payload: " + stringsCount);
            }
            bb.position((4 + length) << 2);//3 headers + 1 count
            // ensure there is enough space for at least that many string lengths left
            if (bb.remaining() < stringsCount << 2) {
                throw new IllegalArgumentException("Payload claims to have " + stringsCount + " strings, but only has space left for " + (bb.remaining() >> 2));
            }
            strings = new String[stringsCount];
            for (int i = 0; i < stringsCount; i++) {
                int stringLength = bb.getInt();
                if (bb.remaining() < stringLength) {
                    throw new IllegalArgumentException("Payload claims to have a string with length " + stringLength + " but only " + bb.remaining() + " bytes remain");
                }
                byte[] bytes = new byte[stringLength];
                bb.get(bytes);
                strings[i] = new String(bytes);
            }
        }



        // move back to the starting point, right after the three headers
//        payload.position(3);//unnecessary
        bb.position(3 << 2);

        // move the limit of the payload to just before strings start (if any)
        payload.limit(length + 3);

        this.strings = strings;
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
        //TODO this could certainly be improved
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
        // Using the exact same math as in GWT2-RPC, could probably be switched to read a double and use those bits?
        // Or read two ints instead of three?
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
        if (claimedTokens + slots > payload.limit() + strings.length) {
            throw new SerializationException("Request claims to be larger than it is");
        }
        claimedTokens += slots;
    }
}

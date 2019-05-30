package org.gwtproject.rpc.serialization.stream.bytebuffer;


import org.gwtproject.rpc.serialization.api.SerializationException;
import org.gwtproject.rpc.serialization.api.TypeSerializer;
import org.gwtproject.rpc.serialization.api.impl.AbstractSerializationStreamReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class ByteBufferSerializationStreamReader extends AbstractSerializationStreamReader {
    private final TypeSerializer serializer;//b
    private final ByteBuffer bb;
    private final String[] strings;

    private int claimedTokens;

    public ByteBufferSerializationStreamReader(TypeSerializer serializer, ByteBuffer bb, String[] strings) {
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.serializer = serializer;
        this.bb = bb;
        this.strings = strings;
        int version = bb.getInt();
        int flags = bb.getInt();
        int length = bb.getInt();
        assert length == bb.remaining();
        setVersion(version);
        setFlags(flags);
    }

    public ByteBufferSerializationStreamReader(TypeSerializer serializer, ByteBuffer bb) {
        bb.order(ByteOrder.LITTLE_ENDIAN);

        this.serializer = serializer;
        this.bb = bb;
        int version = bb.getInt();
        int flags = bb.getInt();
        int length = bb.getInt();
        setVersion(version);
        setFlags(flags);

        //strings are in the payload, read them out first and assign them
        String[] strings = new String[0];
        // see if there is a stringCount, and thus strings present
        if (bb.limit() > (3 << 2) + length) {
            int stringsCount = bb.get((3 << 2) + length);
            if (stringsCount < 1) {
                throw new IllegalArgumentException("Invalid string count in payload: " + stringsCount);
            }
            bb.position((4 << 2) + length);//3 headers + 1 count
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
                strings[i] = new String(bytes, Charset.forName("UTF-8"));
            }
        }

        // move back to the starting point, right after the three headers
        bb.position(3 << 2);

        // move the limit of the payload to just before strings start (if any)
        bb.limit((3 << 2) + length);

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
        return bb.get() == 1;//or zero
    }

    @Override
    public byte readByte() throws SerializationException {
        return bb.get();
    }

    @Override
    public char readChar() throws SerializationException {
        return bb.getChar();
    }

    @Override
    public double readDouble() throws SerializationException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public float readFloat() throws SerializationException {
        return bb.getFloat();
    }

    @Override
    public int readInt() throws SerializationException {
        return bb.getInt();
    }

    @Override
    public long readLong() throws SerializationException {
        return bb.getLong();
    }

    @Override
    public short readShort() throws SerializationException {
        return bb.getShort();
    }

    @Override
    public String readString() throws SerializationException {
        return getString(readInt());
    }

    @Override
    public void claimItems(int slots) throws SerializationException {
        //shift to zero for now to avoid something like a byte[] appearing to be bigger than the entire payload
        if (claimedTokens + slots > (bb.limit() << 0) + strings.length) {
            throw new SerializationException("Request claims to be larger than it is");
        }
        claimedTokens += slots;
    }
}

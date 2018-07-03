package com.vertispan.serial.streams.string;

import com.vertispan.serial.SerializationException;
import com.vertispan.serial.TypeSerializer;
import com.vertispan.serial.impl.AbstractSerializationStreamReader;

import java.util.ArrayList;
import java.util.List;

public class StringSerializationStreamReader extends AbstractSerializationStreamReader {
    private final TypeSerializer serializer;

    private int claimedTokens;

    private int tokenIndex = 0;
    private final List<String> tokens = new ArrayList<>();

    private final List<String> stringTable = new ArrayList<>();

    public StringSerializationStreamReader(TypeSerializer serializer, String payload) {
        this.serializer = serializer;

        int idx = 0, nextIdx;
        while (-1 != (nextIdx = payload.indexOf(RPC_SEPARATOR_CHAR, idx))) {
            String current = payload.substring(idx, nextIdx);
            tokens.add(current);
            idx = nextIdx + 1;
        }

        try {
            // Read the stream version number
            //
            setVersion(readInt());

            // Read the flags from the stream
            //
            setFlags(readInt());

            int stringCount = readInt();
            claimItems(stringCount);

//        List<String> buffer = new ArrayList<String>(stringCount);
            for (int typeNameIndex = 0; typeNameIndex < stringCount; ++typeNameIndex) {
                String str = token();
                // Change quoted characters back.
                idx = str.indexOf('\\');
                if (idx >= 0) {
                    StringBuilder buf = new StringBuilder();
                    int pos = 0;
                    while (idx >= 0) {
                        buf.append(str.substring(pos, idx));
                        if (++idx == str.length()) {
                            throw new SerializationException("Unmatched backslash: \"" + str + "\"");
                        }
                        char ch = str.charAt(idx);
                        pos = idx + 1;
                        switch (ch) {
                            case '0':
                                buf.append('\u0000');
                                break;
                            case '!':
                                buf.append(RPC_SEPARATOR_CHAR);
                                break;
                            case '\\':
                                buf.append(ch);
                                break;
                            case 'u':
                                try {
                                    ch = (char) Integer.parseInt(str.substring(idx + 1, idx + 5), 16);
                                } catch (NumberFormatException e) {
                                    throw new SerializationException("Invalid Unicode escape sequence in \"" + str
                                            + "\"");
                                }
                                buf.append(ch);
                                pos += 4;
                                break;
                            default:
                                throw new SerializationException("Unexpected escape character " + ch
                                        + " after backslash: \"" + str + "\"");
                        }
                        idx = str.indexOf('\\', pos);
                    }
                    buf.append(str.substring(pos));
                    str = buf.toString();
                }
                stringTable.add(str);
            }

            if (stringTable.size() != stringCount) {
                throw new SerializationException("Expected " + stringCount
                        + " string table elements; received " + stringTable.size());
            }
        } catch (SerializationException ex) {
            throw new IllegalStateException("Invalid payload: ", ex);
        }
    }

    @Override
    protected Object deserialize(String typeSignature) throws com.google.gwt.user.client.rpc.SerializationException {
        int id = reserveDecodedObjectIndex();
        Object instance = serializer.instantiate(this, typeSignature);
        rememberDecodedObject(id, instance);
        serializer.deserialize(this, instance, typeSignature);
        return instance;
    }

    @Override
    protected String getString(int index) {
        if (index == 0) {
            return null;
        }
        // index is 1-based
        assert (index > 0);
        assert (index <= stringTable.size());
        return stringTable.get(index - 1);
    }

    private String token() {
      return tokens.get(tokenIndex++);
    }


    @Override
    public boolean readBoolean() throws SerializationException {
        return !token().equals("0");
    }

    @Override
    public byte readByte() throws SerializationException {
        return Byte.parseByte(token());
    }

    @Override
    public char readChar() throws SerializationException {
        return (char) readInt();
    }

    @Override
    public double readDouble() throws SerializationException {
        return Double.parseDouble(token());
    }

    @Override
    public float readFloat() throws SerializationException {
        return (float) readDouble();
    }

    @Override
    public int readInt() throws SerializationException {
        return Integer.parseInt(token());
    }

    @Override
    public long readLong() throws SerializationException {
        return longFromBase64(token());
    }

    @Override
    public short readShort() throws SerializationException {
        return Short.parseShort(token());
    }

    @Override
    public String readString() throws SerializationException {
        return getString(readInt());
    }

    @Override
    public void claimItems(int slots) throws SerializationException {
        if (claimedTokens + slots > tokens.size() + stringTable.size()) {
            throw new SerializationException("Request claims to be larger than it is");
        }
        claimedTokens += slots;
    }
}

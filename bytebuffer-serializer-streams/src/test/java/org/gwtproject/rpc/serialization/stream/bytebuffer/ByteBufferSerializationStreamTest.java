package org.gwtproject.rpc.serialization.stream.bytebuffer;

import org.gwtproject.rpc.serialization.api.FieldSerializer;
import org.gwtproject.rpc.serialization.api.SerializationException;
import org.gwtproject.rpc.serialization.api.TypeSerializer;
import org.gwtproject.rpc.serialization.api.impl.TypeSerializerImpl;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

public class ByteBufferSerializationStreamTest {
    private TypeSerializer t = new TypeSerializerImpl() {
        @Override
        protected FieldSerializer serializer(String name) {
            return null;
        }

        @Override
        public String getChecksum() {
            return "";
        }
    };

    private ByteBufferSerializationStreamWriter getStreamWriter() {
        ByteBufferSerializationStreamWriter writer = new ByteBufferSerializationStreamWriter(t);
        writer.setFlags(0);
        return writer;
    }

    private ByteBufferSerializationStreamReader getSplitPayloadStreamReader(ByteBufferSerializationStreamWriter writer) {
        ByteBufferSerializationStreamReader reader = new ByteBufferSerializationStreamReader(t, writer.getPayloadBytes(), writer.getFinishedStringTable());
        assertEquals(7, reader.getVersion());
        assertEquals(0, reader.getFlags());
        return reader;
    }

    private ByteBufferSerializationStreamReader getSinglePayloadStreamReader(ByteBufferSerializationStreamWriter writer) {
        ByteBufferSerializationStreamReader reader = new ByteBufferSerializationStreamReader(t, writer.getFullPayload());
        assertEquals(7, reader.getVersion());
        assertEquals(0, reader.getFlags());
        return reader;
    }

    @Test
    public void testInt() throws Exception {
        ByteBufferSerializationStreamWriter writer = getStreamWriter();
        writer.writeInt(4);
        writer.writeInt(1);
        writer.writeInt(Integer.MAX_VALUE);
        writer.writeInt(Integer.MIN_VALUE);
        writer.writeInt(1);

        ByteBufferSerializationStreamReader reader = getSplitPayloadStreamReader(writer);

        assertEquals(4, reader.readInt());
        assertEquals(1, reader.readInt());
        assertEquals(Integer.MAX_VALUE, reader.readInt());
        assertEquals(Integer.MIN_VALUE, reader.readInt());
        assertEquals(1, reader.readInt());
    }

    @Test
    public void testLong() throws Exception {
        ByteBufferSerializationStreamWriter writer = getStreamWriter();
        writer.writeLong(4);
        writer.writeLong(1);
        writer.writeLong(Integer.MAX_VALUE);
        writer.writeLong(Integer.MIN_VALUE);
        writer.writeLong(1);
        writer.writeLong(Long.MAX_VALUE);
        writer.writeLong(Long.MIN_VALUE);

        ByteBufferSerializationStreamReader reader = getSplitPayloadStreamReader(writer);

        assertEquals(4L, reader.readLong());
        assertEquals(1L, reader.readLong());
        assertEquals((long)Integer.MAX_VALUE, reader.readLong());
        assertEquals((long)Integer.MIN_VALUE, reader.readLong());
        assertEquals(1L, reader.readLong());
        assertEquals(Long.MAX_VALUE, reader.readLong());
        assertEquals(Long.MIN_VALUE, reader.readLong());
    }


    @Test
    public void testString() throws Exception {
        ByteBufferSerializationStreamWriter writer = getStreamWriter();

        writer.writeString("foo");
        writer.writeString("foo1");
        writer.writeString("foo");
        writer.writeString("bar");

        ByteBufferSerializationStreamReader reader = getSplitPayloadStreamReader(writer);

        assertEquals("foo", reader.readString());
        assertEquals("foo1", reader.readString());
        assertEquals("foo", reader.readString());
        assertEquals("bar", reader.readString());
    }



    @Test
    public void testFloat() throws Exception {
        ByteBufferSerializationStreamWriter writer = getStreamWriter();

        writer.writeFloat(1.23f);
        writer.writeFloat(3);
        writer.writeFloat(4);
        writer.writeFloat(1.23f);
        writer.writeFloat(Float.MAX_VALUE);
        writer.writeFloat(Float.MIN_VALUE);
        writer.writeFloat(Float.MIN_NORMAL);
        writer.writeFloat(Float.NaN);
        writer.writeFloat(Float.NEGATIVE_INFINITY);
        writer.writeFloat(Float.POSITIVE_INFINITY);

        ByteBufferSerializationStreamReader reader = getSplitPayloadStreamReader(writer);

        assertEquals(1.23f, reader.readFloat(), 0);
        assertEquals(3.0f, reader.readFloat(), 0);
        assertEquals(4.0f, reader.readFloat(), 0);
        assertEquals(1.23f, reader.readFloat(), 0);
        assertEquals(Float.MAX_VALUE, reader.readFloat(), 0);
        assertEquals(Float.MIN_VALUE, reader.readFloat(), 0);
        assertEquals(Float.MIN_NORMAL, reader.readFloat(), 0);
        assertTrue(Float.isNaN(reader.readFloat()));
        assertEquals(Float.NEGATIVE_INFINITY, reader.readFloat(), 0);
        assertEquals(Float.POSITIVE_INFINITY, reader.readFloat(), 0);

    }


    @Test
    public void testDouble() throws Exception {
        ByteBufferSerializationStreamWriter writer = getStreamWriter();

        writer.writeDouble(1.23);
        writer.writeDouble(3);
        writer.writeDouble(4);
        writer.writeDouble(1.23);
        writer.writeDouble(Double.MAX_VALUE);
        writer.writeDouble(Double.MIN_VALUE);
        writer.writeDouble(Double.MIN_NORMAL);
        writer.writeDouble(Double.NaN);
        writer.writeDouble(Double.NEGATIVE_INFINITY);
        writer.writeDouble(Double.POSITIVE_INFINITY);

        ByteBufferSerializationStreamReader reader = getSplitPayloadStreamReader(writer);

        assertEquals(1.23, reader.readDouble(), 0);
        assertEquals(3.0, reader.readDouble(), 0);
        assertEquals(4.0, reader.readDouble(), 0);
        assertEquals(1.23, reader.readDouble(), 0);
        assertEquals(Double.MAX_VALUE, reader.readDouble(), 0);
        assertEquals(Double.MIN_VALUE, reader.readDouble(), 0);
        assertEquals(Double.MIN_NORMAL, reader.readDouble(), 0);
        assertTrue(Double.isNaN(reader.readDouble()));
        assertEquals(Double.NEGATIVE_INFINITY, reader.readDouble(), 0);
        assertEquals(Double.POSITIVE_INFINITY, reader.readDouble(), 0);

    }

    //just the one test with split payloads for primitives, since no primitives use strings
    @Test
    public void testSinglePayloadInt() throws Exception {
        ByteBufferSerializationStreamWriter writer = getStreamWriter();
        writer.writeInt(4);
        writer.writeInt(1);
        writer.writeInt(Integer.MAX_VALUE);
        writer.writeInt(Integer.MIN_VALUE);
        writer.writeInt(1);

        ByteBufferSerializationStreamReader reader = getSinglePayloadStreamReader(writer);

        assertEquals(4, reader.readInt());
        assertEquals(1, reader.readInt());
        assertEquals(Integer.MAX_VALUE, reader.readInt());
        assertEquals(Integer.MIN_VALUE, reader.readInt());
        assertEquals(1, reader.readInt());
    }

    @Test
    public void testSinglePayloadString() throws Exception {
        ByteBufferSerializationStreamWriter writer = getStreamWriter();

        writer.writeString("foo");
        writer.writeString("foo1");
        writer.writeString("foo");
        writer.writeString("bar");
        writer.writeString("\"");
        writer.writeString("!");
        writer.writeString("|");
        writer.writeString("\\");
        writer.writeString("\\\\");
        writer.writeString("✓");
        writer.writeString("\0");

        ByteBufferSerializationStreamReader reader = getSinglePayloadStreamReader(writer);

        assertEquals("foo", reader.readString());
        assertEquals("foo1", reader.readString());
        assertEquals("foo", reader.readString());
        assertEquals("bar", reader.readString());
        assertEquals("\"", reader.readString());
        assertEquals("!", reader.readString());
        assertEquals("|", reader.readString());
        assertEquals("\\", reader.readString());
        assertEquals("\\\\", reader.readString());
        assertEquals("✓", reader.readString());
        assertEquals("\0", reader.readString());
    }

    @Test
    public void testManyStrings() throws SerializationException {
        ByteBufferSerializationStreamWriter writer = getStreamWriter();

        for (int i = 0; i < 128; i++) {
            writer.writeString("string #" + i);
        }

        ByteBufferSerializationStreamReader reader = getSinglePayloadStreamReader(writer);
        for (int i = 0; i < 128; i++) {
            assertEquals("string #" + i, reader.readString());
        }
    }

    @Test
    public void testLongStrings() throws SerializationException {
        // if the test runs out of memory, this is too high - 28 lets us test a up to a 134mb string in a total payload
        // of 270mb or so, which is a decent test of the capabilities of the stream and should not stretch a test JVM.
        int maxLengthStringPowOf2 = 28;

        ByteBufferSerializationStreamWriter writer = getStreamWriter();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLengthStringPowOf2; i++) {
            for (int j = Math.max(0, (int) Math.pow(2, i - 1)); j < Math.pow(2, i); j++) {
                sb.append(j % 10);
            }
            writer.writeString(sb.toString());
        }

        ByteBufferSerializationStreamReader reader = getSinglePayloadStreamReader(writer);

        for (int i = 0; i < maxLengthStringPowOf2; i++) {
            assertEquals(reader.readString().length(), (int)Math.pow(2, i));
        }
    }

}
package com.vertispan.serial.streams.bytebuffer;

import com.vertispan.serial.FieldSerializer;
import com.vertispan.serial.TypeSerializer;
import com.vertispan.serial.impl.TypeSerializerImpl;
import org.junit.Test;

import static org.junit.Assert.*;

public class ByteBufferSerializationStreamTest {
    private TypeSerializer t = new TypeSerializerImpl() {
        @Override
        protected FieldSerializer serializer(String name) {
            return null;
        }
    };

    private ByteBufferSerializationStreamWriter getUncompressedStreamWriter() {
        ByteBufferSerializationStreamWriter writer = new ByteBufferSerializationStreamWriter(t);
        writer.setFlags(0);
        return writer;
    }

    private ByteBufferSerializationStreamReader getStreamReader(ByteBufferSerializationStreamWriter writer) {
        return new ByteBufferSerializationStreamReader(t, writer.getPayloadBytes(), writer.getFinishedStringTable());
    }

    @Test
    public void testInt() throws Exception {
        ByteBufferSerializationStreamWriter writer = getUncompressedStreamWriter();
        writer.writeInt(4);
        writer.writeInt(1);
        writer.writeInt(Integer.MAX_VALUE);
        writer.writeInt(Integer.MIN_VALUE);
        writer.writeInt(1);

        ByteBufferSerializationStreamReader reader = getStreamReader(writer);

        assertEquals(4, reader.readInt());
        assertEquals(1, reader.readInt());
        assertEquals(Integer.MAX_VALUE, reader.readInt());
        assertEquals(Integer.MIN_VALUE, reader.readInt());
        assertEquals(1, reader.readInt());
    }


    @Test
    public void testLong() throws Exception {
        ByteBufferSerializationStreamWriter writer = getUncompressedStreamWriter();
        writer.writeLong(4);
        writer.writeLong(1);
        writer.writeLong(Integer.MAX_VALUE);
        writer.writeLong(Integer.MIN_VALUE);
        writer.writeLong(1);
        writer.writeLong(Long.MAX_VALUE);
        writer.writeLong(Long.MIN_VALUE);

        ByteBufferSerializationStreamReader reader = getStreamReader(writer);

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
        ByteBufferSerializationStreamWriter writer = getUncompressedStreamWriter();

        writer.writeString("foo");
        writer.writeString("foo1");
        writer.writeString("foo");
        writer.writeString("bar");

        ByteBufferSerializationStreamReader reader = getStreamReader(writer);

        assertEquals("foo", reader.readString());
        assertEquals("foo1", reader.readString());
        assertEquals("foo", reader.readString());
        assertEquals("bar", reader.readString());
    }



    @Test
    public void testFloat() throws Exception {
        ByteBufferSerializationStreamWriter writer = getUncompressedStreamWriter();

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

        ByteBufferSerializationStreamReader reader = getStreamReader(writer);

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
        ByteBufferSerializationStreamWriter writer = getUncompressedStreamWriter();

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

        ByteBufferSerializationStreamReader reader = getStreamReader(writer);

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

}
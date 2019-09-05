package com.google.gwt.user.client.rpc.core.java.math;

import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import org.gwtproject.rpc.serialization.api.FieldSerializer;
import org.gwtproject.rpc.serialization.api.SerializationStreamReader;
import org.gwtproject.rpc.serialization.api.TypeSerializer;
import org.gwtproject.rpc.serialization.api.impl.TypeSerializerImpl;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamWriter;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class BigIntegerSerializerTest {

    private final TypeSerializer t = new TypeSerializerImpl() {
        @Override
        protected FieldSerializer serializer(String name) {
            return null;
        }

        @Override
        public String getChecksum() {
            return "";
        }
    };

    private SerializationStreamWriter getByteBufferWriter() {
        return new ByteBufferSerializationStreamWriter(t);
    }

    private SerializationStreamReader getByteBufferReader(SerializationStreamWriter writer) {
        return new ByteBufferSerializationStreamReader(t, ((ByteBufferSerializationStreamWriter)writer).getPayloadBytes());
    }

    private SerializationStreamWriter getStringWriter() {
        final StringSerializationStreamWriter writer = new StringSerializationStreamWriter(t);
        writer.prepareToWrite();
        return writer;
    }

    private SerializationStreamReader getStringReader(SerializationStreamWriter writer) {
        return new StringSerializationStreamReader(t, writer.toString());
    }

    private void testRoundTrip(Supplier<SerializationStreamWriter> writerSupplier,
                               Function<SerializationStreamWriter,SerializationStreamReader> readerFunction) throws Exception {

        final SerializationStreamWriter writer = writerSupplier.get();
        BigInteger_CustomFieldSerializer.serialize(writer, BigInteger.ONE);
        BigInteger_CustomFieldSerializer.serialize(writer, BigInteger.ZERO);
        BigInteger_CustomFieldSerializer.serialize(writer, BigInteger.TEN);
        BigInteger_CustomFieldSerializer.serialize(writer, BigInteger.valueOf(-1));
        BigInteger_CustomFieldSerializer.serialize(writer, BigInteger.valueOf(Long.MAX_VALUE));
        BigInteger_CustomFieldSerializer.serialize(writer, BigInteger.valueOf(Long.MIN_VALUE));

        final SerializationStreamReader reader = readerFunction.apply(writer);
        Assert.assertEquals(BigInteger.ONE, BigInteger_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigInteger.ZERO, BigInteger_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigInteger.TEN, BigInteger_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigInteger.valueOf(-1), BigInteger_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigInteger.valueOf(Long.MAX_VALUE), BigInteger_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigInteger.valueOf(Long.MIN_VALUE), BigInteger_CustomFieldSerializer.instantiate(reader));
    }

    @Test
    public void testByteBufferRoundTrip() throws Exception  {
        testRoundTrip(this::getByteBufferWriter, this::getByteBufferReader);
    }

    @Test
    public void testStringRoundTrip() throws Exception  {
        testRoundTrip(this::getStringWriter, this::getStringReader);
    }
}

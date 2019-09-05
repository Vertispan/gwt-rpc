package org.gwtproject.rpc.core.java.math;

import org.gwtproject.rpc.serialization.api.FieldSerializer;
import org.gwtproject.rpc.serialization.api.SerializationStreamReader;
import org.gwtproject.rpc.serialization.api.SerializationStreamWriter;
import org.gwtproject.rpc.serialization.api.TypeSerializer;
import org.gwtproject.rpc.serialization.api.impl.TypeSerializerImpl;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamWriter;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.function.Function;
import java.util.function.Supplier;

public class BigDecimalSerializerTest {

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
        BigDecimal_CustomFieldSerializer.serialize(writer, BigDecimal.ONE);
        BigDecimal_CustomFieldSerializer.serialize(writer, BigDecimal.ZERO);
        BigDecimal_CustomFieldSerializer.serialize(writer, BigDecimal.TEN);
        BigDecimal_CustomFieldSerializer.serialize(writer, BigDecimal.valueOf(-1));
        BigDecimal_CustomFieldSerializer.serialize(writer, BigDecimal.valueOf(Long.MAX_VALUE));
        BigDecimal_CustomFieldSerializer.serialize(writer, BigDecimal.valueOf(Long.MIN_VALUE));
        BigDecimal_CustomFieldSerializer.serialize(writer, BigDecimal.valueOf(Math.PI));
        BigDecimal_CustomFieldSerializer.serialize(writer, BigDecimal.valueOf(-Math.PI));

        final SerializationStreamReader reader = readerFunction.apply(writer);
        Assert.assertEquals(BigDecimal.ONE, BigDecimal_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigDecimal.ZERO, BigDecimal_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigDecimal.TEN, BigDecimal_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigDecimal.valueOf(-1), BigDecimal_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigDecimal.valueOf(Long.MAX_VALUE), BigDecimal_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigDecimal.valueOf(Math.PI), BigDecimal_CustomFieldSerializer.instantiate(reader));
        Assert.assertEquals(BigDecimal.valueOf(-Math.PI), BigDecimal_CustomFieldSerializer.instantiate(reader));
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

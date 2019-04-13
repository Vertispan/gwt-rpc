package com.example.rpcsample.client;

import com.example.rpcsample.shared.*;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamWriter;
import elemental2.dom.DomGlobal;

public class SimpleAppEntrypoint implements EntryPoint {
    @Override
    public void onModuleLoad() {
        OrderSerializer serializer = new OrderSerializer_Impl();
        StringSerializationStreamWriter serializationStreamWriter = new StringSerializationStreamWriter(
                serializer.createSerializer()
        );
        serializationStreamWriter.prepareToWrite();
        Invoice invoice = new Invoice();
        invoice.setClient(new Company());
        invoice.getClient().setName("The Company Inc");
        invoice.getClient().setAddress(new Address("1234 Main St", "Someville", "City", "Country", "123456"));
        serializer.writeInvoice(invoice, serializationStreamWriter);

        DomGlobal.console.log(serializationStreamWriter.toString());
    }
}

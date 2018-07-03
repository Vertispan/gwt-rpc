package com.vertispan.sample.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.vertispan.sample.shared.*;
import com.vertispan.serial.streams.string.StringSerializationStreamWriter;
import elemental2.dom.DomGlobal;

public class SimpleAppEntrypoint implements EntryPoint {
    @Override
    public void onModuleLoad() {
        OrderSerializer serializer = new OrderSerializer_Impl();
        StringSerializationStreamWriter serializationStreamWriter = new StringSerializationStreamWriter(
                serializer.createSerializer(),
                //moduleBaseURL - the url that this .nocache.js is found in. may not be required at all without the policy file
                GWT.getModuleBaseURL(),
                //serializationPolicyStringName - should be generated if it exists at all, since the server doesnt use reflection any more
                "1234567890abcdef1234567890abcdef"
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

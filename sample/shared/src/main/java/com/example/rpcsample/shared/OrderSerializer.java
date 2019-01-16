package com.example.rpcsample.shared;

import org.gwtproject.rpc.serialization.api.SerializationStreamReader;
import org.gwtproject.rpc.serialization.api.SerializationStreamWriter;
import org.gwtproject.rpc.serialization.api.SerializationWiring;
import org.gwtproject.rpc.serialization.api.TypeSerializer;

@SerializationWiring
public interface OrderSerializer {
    static OrderSerializer create() {
        return new OrderSerializer_Impl();
    }

    TypeSerializer createSerializer();

    void writeInvoice(Invoice invoice, SerializationStreamWriter writer);


    Project readProject(SerializationStreamReader reader);

//    void writeLocation(HasAddress hasAddress, SerializationStreamWriter writer);
}

package com.vertispan.sample.shared;

import com.vertispan.serial.SerializationStreamReader;
import com.vertispan.serial.SerializationStreamWriter;
import com.vertispan.serial.SerializationWiring;
import com.vertispan.serial.TypeSerializer;

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

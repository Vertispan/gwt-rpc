package com.vertispan.sample.shared;

import com.vertispan.serial.SerializationStreamReader;
import com.vertispan.serial.SerializationStreamWriter;
import com.vertispan.serial.SerializationWiring;
import com.vertispan.serial.Serializer;

@SerializationWiring
public interface OrderSerializer {
    Serializer createSerializer();


    void writeInvoice(Invoice invoice, SerializationStreamWriter writer);


    Project readProject(SerializationStreamReader reader);
}

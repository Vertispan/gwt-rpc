/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gwtproject.rpc.core.java.math;

import com.google.gwt.user.client.rpc.SerializationException;
import org.gwtproject.rpc.serialization.api.SerializationStreamReader;
import org.gwtproject.rpc.serialization.api.SerializationStreamWriter;

import java.math.BigInteger;

/**
 * Custom field serializer for BigInteger.
 */
public class BigInteger_CustomFieldSerializer {

    /**
     * @param streamReader a SerializationStreamReader instance
     * @param instance the instance to be deserialized
     */
    public static void deserialize(SerializationStreamReader streamReader,
                                   BigInteger instance) {
    }

    public static BigInteger instantiate(SerializationStreamReader streamReader)
            throws SerializationException {
        final int length = streamReader.readInt();
        final byte[] unscaledValue = new byte[length];
        for (int i = 0; i < length; i++) {
            unscaledValue[i] = streamReader.readByte();
        }
        return new BigInteger(unscaledValue);
    }

    public static void serialize(SerializationStreamWriter streamWriter,
                                 BigInteger instance) throws SerializationException {
        final byte[] unscaledValue = instance.toByteArray();
        streamWriter.writeInt(unscaledValue.length);
        for (int i = 0; i < unscaledValue.length; i++) {
            streamWriter.writeByte(unscaledValue[i]);
        }
    }
}

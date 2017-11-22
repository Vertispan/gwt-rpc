/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client.rpc.core.java.util;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.vertispan.serial.SerializationException;
import com.vertispan.serial.SerializationStreamReader;
import com.vertispan.serial.SerializationStreamWriter;

import java.util.List;

/**
 * Dummy class for nesting the custom serializer.
 */
public final class Arrays {

  /**
   * Custom field serializer for {@link java.util.Arrays.ArrayList}.
   */
  @SuppressWarnings("rawtypes")
  public static final class ArrayList_CustomFieldSerializer {

    public static String concreteType() {
      return java.util.Arrays.asList().getClass().getName();
    }

    @SuppressWarnings("unused")
    public static void deserialize(SerializationStreamReader streamReader,
        List<?> instance) throws SerializationException {
      // Handled in instantiate.
    }

    public static List<?> instantiate(SerializationStreamReader streamReader)
        throws SerializationException, com.google.gwt.user.client.rpc.SerializationException {
      int size = streamReader.readInt();
      streamReader.claimItems(size);
      Object[] array = new Object[size];
      for (int i = 0; i < size; ++i) {
        array[i] = streamReader.readObject();
      }
      return java.util.Arrays.asList(array);
    }

    public static void serialize(SerializationStreamWriter streamWriter,
        List<?> instance) throws SerializationException, com.google.gwt.user.client.rpc.SerializationException {
      int size = instance.size();
      streamWriter.writeInt(size);
      for (Object obj : instance) {
        streamWriter.writeObject(obj);
      }
    }
  }
}

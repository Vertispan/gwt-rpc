/*
 * #%L
 * workers
 * %%
 * Copyright (C) 2011 - 2018 Vertispan LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.colinalworth.gwt.worker.client.impl;

import com.colinalworth.gwt.websockets.shared.impl.AbstractEndpointImpl;
import com.colinalworth.gwt.worker.client.MessagePortEndpoint;
import com.vertispan.serial.SerializationStreamReader;
import com.vertispan.serial.SerializationStreamWriter;
import com.vertispan.serial.TypeSerializer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base implementation of Endpoint
 */
public abstract class AbstractWorkerEndpointImpl<E extends MessagePortEndpoint<?>>
		extends AbstractEndpointImpl implements MessagePortEndpoint<E> {
	private E remote;

	protected <W extends SerializationStreamWriter> AbstractWorkerEndpointImpl(
			Function<TypeSerializer, W> writerFactory,
			Consumer<W> send,
			TypeSerializer serializer,
			BiConsumer<Consumer<SerializationStreamReader>, TypeSerializer> onMessage
	) {
		super(writerFactory, send, serializer, onMessage);
	}

	@Override
	public void setRemote(E remote) {
		this.remote = remote;
	}

	@Override
	public E getRemote() {
		return remote;
	}

}


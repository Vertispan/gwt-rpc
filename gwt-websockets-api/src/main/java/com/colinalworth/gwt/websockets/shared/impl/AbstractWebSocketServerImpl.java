/*
 * #%L
 * gwt-websockets-api
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
package com.colinalworth.gwt.websockets.shared.impl;

import com.colinalworth.gwt.websockets.shared.Client;
import com.colinalworth.gwt.websockets.shared.Server;
import com.vertispan.serial.SerializationStreamReader;
import com.vertispan.serial.SerializationStreamWriter;
import com.vertispan.serial.TypeSerializer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractWebSocketServerImpl<S extends Server<S,C>, C extends Client<C,S>>
		extends AbstractEndpointImpl implements Server<S, C> {

	private C client;
	protected <W extends SerializationStreamWriter> AbstractWebSocketServerImpl(
			Function<TypeSerializer, W> writerFactory,
			Consumer<W> send,
			TypeSerializer serializer,
			BiConsumer<Consumer<SerializationStreamReader>, TypeSerializer> onMessage
	) {
		super(writerFactory, send, serializer, onMessage);
	}

	@Override
	public void onOpen(Connection connection, C client) {
		throw new UnsupportedOperationException("Cannot be called from client code");
	}

	@Override
	public void onClose(Connection connection, C client) {
		throw new UnsupportedOperationException("Cannot be called from client code");
	}

	@Override
	public void onError(Throwable error) {
		throw new UnsupportedOperationException("Cannot be called from client code");
	}

	@Override
	public C getClient() {
		return client;
	}

	@Override
	public void setClient(C client) {
		this.client = client;
	}

	@Override
	public void close() {
		//TODO
	}
}

/*
 * #%L
 * gwt-websockets-jsr356
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
package com.colinalworth.gwt.websockets.server;

import com.colinalworth.gwt.websockets.shared.Client;
import com.colinalworth.gwt.websockets.shared.Server;
import com.colinalworth.gwt.websockets.shared.Server.Connection;
import com.colinalworth.gwt.websockets.shared.impl.AbstractEndpointImpl.EndpointImplConstructor;
import com.vertispan.serial.streams.string.StringSerializationStreamReader;
import com.vertispan.serial.streams.string.StringSerializationStreamWriter;

import javax.websocket.MessageHandler.Whole;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import java.io.IOException;
import java.util.function.Consumer;

public class RpcEndpoint<S extends Server<S, C>, C extends Client<C, S>> {
	private final S server;
	private final EndpointImplConstructor<C> clientConstructor;

	private Consumer<String> handleMessage;

	public RpcEndpoint(S server, EndpointImplConstructor<C> clientConstructor) {
		this.server = server;
		this.clientConstructor = clientConstructor;
	}

	RpcEndpoint(EndpointImplConstructor<C> clientConstructor) {
		this.server = (S) this;
		this.clientConstructor = clientConstructor;
	}


	@OnOpen
	public void onOpen(Session session) {
		C instance = clientConstructor.create(
				serializer -> {
					StringSerializationStreamWriter writer = new StringSerializationStreamWriter(serializer, "", "");
					writer.prepareToWrite();
					return writer;
				},
				writer -> session.getAsyncRemote().sendText(writer.toString()),
				(onMessage, serializer) -> {
					// using this to delegate to OnMessage, not working otherwise
					handleMessage = message -> onMessage.accept(new StringSerializationStreamReader(serializer, message));
				}
		);
		server.setClient(instance);
		instance.setServer(server);

		session.setMaxIdleTimeout(0);
		server.onOpen(new Jsr356Connection(session), server.getClient());
	}

	@OnMessage
	public void onMessage(String message) {
		handleMessage.accept(message);
	}

	@OnClose
	public void onClose(Session session) {
		assert server.getClient() != null;
		server.onClose(new Jsr356Connection(session), server.getClient());
	}

	@OnError
	public void onError(Throwable thr) {
		if (server != this) {
			server.onError(thr);
		}
	}

	private static class Jsr356Connection implements Connection {
		private final Session session;

		private Jsr356Connection(Session session) {
			this.session = session;
		}

		@Override
		public void data(String key, Object value) {
			session.getUserProperties().put(key, value);
		}

		@Override
		public Object data(String key) {
			return session.getUserProperties().get(key);
		}

		@Override
		public void close() {
			try {
				session.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}

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
package org.gwtproject.rpc.servlet.websocket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.gwtproject.rpc.api.Client;
import org.gwtproject.rpc.api.Server;
import org.gwtproject.rpc.api.Server.Connection;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl.EndpointImplConstructor;
import org.gwtproject.rpc.api.impl.AbstractWebSocketClientImpl;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;

/**
 * <p>A server side EndPoint implementation using JSR356 websockets.</p>
 * 
 * <p>It may be necessary to adjust some of the default container settings especially setMaxBinaryMessageBufferSize.
 *    This can be done by accessing the Session or EndPointConfig objects from the Connection object (typically obtained in Server.onOpen()):</p>
 * <pre><code>
 *      ((Jsr356Connection)connection).getSession().setMaxBinaryMessageBufferSize(someValue);
 * </code></pre>
 * 
 * <p>On open and on close events are delegated to the {@link Server}.onOpen and {@link Server}.onClose implementations.</p>
 * 
 */
public class RpcEndpoint<S extends Server<S, C>, C extends Client<C, S>> {
	private final S server;
	private final EndpointImplConstructor<C> clientConstructor;

	private Consumer<ByteBuffer> handleMessage;

	public RpcEndpoint(S server, EndpointImplConstructor<C> clientConstructor) {
		this.server = server;
		this.clientConstructor = clientConstructor;
	}

	@SuppressWarnings("unchecked")
    RpcEndpoint(EndpointImplConstructor<C> clientConstructor) {
		this.server = (S) this;
		this.clientConstructor = clientConstructor;
	}


	@OnOpen
	public void onOpen(Session session, EndpointConfig endpointConfig) {
		C instance = clientConstructor.create(
				serializer -> {
					ByteBufferSerializationStreamWriter writer = new ByteBufferSerializationStreamWriter(serializer);
					writer.prepareToWrite();
					return writer;
				},
				writer -> session.getAsyncRemote().sendBinary(writer.getFullPayload()),
				(onMessage, serializer) -> {
					// using this to delegate to OnMessage, not working otherwise
					handleMessage = message -> onMessage.accept(new ByteBufferSerializationStreamReader(serializer, message));
				}
		);
		List<String> hash = session.getRequestParameterMap().get("checksum");
		try {
			if (hash == null || hash.isEmpty()) {
				session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "The checksum query parameter was not specified, cannot accept request"));
				return;
			} else if (hash.size() > 1) {
				session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "More than one checksum query parameter specified, cannot accept request"));
				return;
			} else {
				String expected = ((AbstractWebSocketClientImpl<?, ?>) instance).getChecksum();
				String actual = hash.iterator().next();
				if (!expected.equals(actual)) {
					session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Expected checksum with value " + expected));
					return;
				}
			}
		} catch (IOException e) {
			onError(new IOException("Error when closing new connection", e));
		}

		server.setClient(instance);
		instance.setServer(server);

        // Configure defaults present in some servlet containers to avoid some confusing limits. Subclasses
        // can override this method to control those defaults on their own.
        session.setMaxIdleTimeout(0);
        
		server.onOpen(new Jsr356Connection(session, null), server.getClient());
	}

	@OnMessage
	public void onMessage(String message, Session session) throws IOException {
		session.getBasicRemote().sendText("Error: This endpoint does not accept string messages, use binary messages instead.");

		session.close();
	}

	@OnMessage
	public void onMessage(ByteBuffer message) {
		handleMessage.accept(message);
	}
	
	@OnClose
	public void onClose(Session session) {
		assert server.getClient() != null;
		server.onClose(new Jsr356Connection(session, null), server.getClient());
	}

	@OnError
	public void onError(Throwable thr) {
		if (server != this) {
			server.onError(thr);
		}
	}

	public static class Jsr356Connection implements Connection {
		private final Session session;
        private final EndpointConfig endpointConfig;

		private Jsr356Connection(Session session, EndpointConfig endpointConfig) {
			this.session = session;
            this.endpointConfig = endpointConfig;
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
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public void close(int closeCode, String closeReason) {
			try {
				session.close(new CloseReason(CloseReason.CloseCodes.getCloseCode(closeCode), closeReason));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

        /**
         * Expose the underlying JS356 EndPointConfig object. This is not available when onClose is called.
         * 
         * @return
         */
        public Optional<EndpointConfig> getEndpointConfig() {
            return Optional.ofNullable(endpointConfig);
        }

		/**
		 * Expose the underlying JS356 Session object.
		 * 
		 * @return
		 */
		public Session getSession() {
            return session;
        }
		
	}

}

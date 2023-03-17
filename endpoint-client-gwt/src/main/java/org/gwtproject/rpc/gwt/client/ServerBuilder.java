/*
 * #%L
 * rpc-client-common
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
package org.gwtproject.rpc.gwt.client;

import elemental2.core.ArrayBuffer;
import elemental2.core.Int8Array;
import elemental2.dom.DomGlobal;
import elemental2.dom.WebSocket;
import elemental2.dom.WebSocket.OnopenFn;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import org.gwtproject.nio.TypedArrayHelper;
import org.gwtproject.rpc.api.Server;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl.EndpointImplConstructor;
import org.gwtproject.rpc.api.impl.AbstractWebSocketServerImpl;
import org.gwtproject.rpc.gwt.client.impl.ServerBuilderImpl;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Base interface to be extended and given a concrete Server interface in a client project,
 * causing code to be generated to connect to a websocket server.
 */
public interface ServerBuilder<S extends Server<? super S, ?>> {
	/**
	 * Sets the full url, including protocol, host, port, and path for the next server connection
	 * to be started. If called with a non-null value, will override the other setters in this
	 * builder.
	 *
	 * @param url
	 * @return
	 */
	ServerBuilder<S> setUrl(String url);

	/**
	 * Sets the path for the next server to be started. Defaults to the RemoteServiceRelativePath
	 * annotation value for the Server interface, if any.
	 *
	 * @param path
	 * @return
	 */
	ServerBuilder<S> setPath(String path);

	/**
	 * Sets the port of the next server instance to be started. Defaults to the port the current
	 * page loaded from.
	 *
	 * @param port
	 * @return
	 */
	ServerBuilder<S> setPort(int port);

	/**
	 * Sets the hostname for the next server to be started. Defaults to the hostname the current
	 * page loaded from.
	 *
	 * @param hostname
	 * @return
	 */
	ServerBuilder<S> setHostname(String hostname);

	/**
	 * Sets the protocol ("ws" or "wss") to connect with. Defaults to wss if the current page
	 * loaded using https, and ws otherwise.
	 *
	 * @param protocol
	 * @return
	 */
	ServerBuilder<S> setProtocol(String protocol);

	/**
	 * Creates a new instance of the specified server type, starts, and returns it. May
	 * be called more than once to create additional connections, such as after the first
	 * is closed.
	 *
	 * @return
	 */
	S start();

	/**
	 * Specifies a handler to receive errors when a problem occurs around the protocol: the connection,
	 * serialization, or other unhandled issues.
	 * @param errorHandler the handler to send connection errors to
	 */
	void setConnectionErrorHandler(ConnectionErrorHandler errorHandler);

	/**
	 * Allows de/serialization and connection problems to be handled rather than rethrowing
	 * or just pushing to GWT.log.
	 */
	public interface ConnectionErrorHandler {
		void onError(Object ex);
	}

	/**
	 * Simple create method that takes the generated server endpoint's constructor and returns a functioning
	 * server builder.
	 */
	static <E extends Server<? super E, ?>> ServerBuilder<E> of(EndpointImplConstructor<E> constructor) {
		return new ServerBuilderImpl<E>() {

			private WebSocket socket;
			private Consumer<ArrayBuffer> onmessage;

			@Override
			public E start() {
				E instance = constructor.create(
						serializer -> {
							ByteBufferSerializationStreamWriter writer = new ByteBufferSerializationStreamWriter(serializer);
							writer.prepareToWrite();
							return writer;
						},
						stream -> socket.send(Js.<Int8Array>uncheckedCast(TypedArrayHelper.unwrap(stream.getFullPayload()))),
						(send, serializer) -> {
							onmessage = message -> {
								ByteBuffer bb = TypedArrayHelper.wrap(message);
								send.accept(new ByteBufferSerializationStreamReader(serializer, bb));
							};
						}
				);

				socket = new WebSocket(getUrl() + "?checksum=" + ((AbstractWebSocketServerImpl<?, ?>) instance).getChecksum());
				socket.binaryType = "arraybuffer";
				socket.onclose = e -> {
					int closeCode = Js.asPropertyMap(e).getAsAny("code").asInt();
					String closeReason = Js.asPropertyMap(e).getAsAny("reason").asString();
					instance.getClient().onClose(closeCode, closeReason);
				};
				socket.onopen = e -> {
					instance.getClient().onOpen();
				};
				socket.onmessage = event -> {
					onmessage.accept((ArrayBuffer) event.data);
				};
				Js.<JsPropertyMap<OnopenFn>>cast(socket).set("onerror", e -> {
					if (getErrorHandler() != null) {
						getErrorHandler().onError(e);
					} else {
						DomGlobal.console.log("A transport error occurred - pass a error handler to your server builder to handle this yourself", e);
					}
				});
				((AbstractWebSocketServerImpl<?, ?>) instance).close = socket::close;
				return instance;
			}
		};
	}
}

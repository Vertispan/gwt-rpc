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
package com.colinalworth.gwt.websockets.client;

import com.colinalworth.gwt.websockets.client.impl.ServerBuilderImpl;
import com.colinalworth.gwt.websockets.shared.Server;
import com.colinalworth.gwt.websockets.shared.impl.AbstractEndpointImpl.EndpointImplConstructor;
import com.vertispan.serial.streams.string.StringSerializationStreamReader;
import com.vertispan.serial.streams.string.StringSerializationStreamWriter;
import elemental2.dom.DomGlobal;
import elemental2.dom.WebSocket;
import elemental2.dom.WebSocket.OnopenFn;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;

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
		return new ServerBuilderImpl<E>("", "") {
			@Override
			public E start() {
				WebSocket socket = new WebSocket(getUrl());
				E instance = constructor.create(
						serializer -> {
							StringSerializationStreamWriter writer = new StringSerializationStreamWriter(serializer, "", "");
							writer.prepareToWrite();
							return writer;
						},
						stream -> socket.send(stream.toString()),
						(send, serializer) -> {
							socket.onmessage = message -> {
								send.accept(new StringSerializationStreamReader(serializer, message.data.toString()));
								return null;
							};
						}
				);
				socket.onclose = e -> {
					instance.getClient().onClose();
					return null;
				};
				socket.onopen = e -> {
					instance.getClient().onOpen();
					return null;
				};
				Js.<JsPropertyMap<OnopenFn>>cast(socket).set("onerror", e -> {
					if (getErrorHandler() != null) {
						getErrorHandler().onError(e);
					} else {
						DomGlobal.console.log("A transport error occurred - pass a error handler to your server builder to handle this yourself", e);
					}
					return null;
				});
				return instance;
			}
		};
	}
}

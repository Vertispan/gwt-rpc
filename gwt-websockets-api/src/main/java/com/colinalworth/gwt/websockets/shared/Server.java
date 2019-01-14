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
package com.colinalworth.gwt.websockets.shared;

import com.colinalworth.gwt.websockets.shared.Endpoint.BaseClass;
import com.colinalworth.gwt.websockets.shared.Endpoint.RemoteEndpointSupplier;
import com.colinalworth.gwt.websockets.shared.impl.AbstractWebSocketServerImpl;

@BaseClass(AbstractWebSocketServerImpl.class)
public interface Server<S extends Server<S,C>, C extends Client<C,S>> {

	/**
	 * Called when a client has initiated a connection to the server. Not to be invoked directly
	 * by the client.
	 * @param connection
	 * @param client
	 */
	void onOpen(Connection connection, C client);

	/**
	 * Called as a client connection is lost. Not to be invoked directly by the client.
	 *
	 * @param connection
	 * @param client
	 */
	void onClose(Connection connection, C client);

	void onError(Throwable error);

	/**
	 * Gets the client for the currently running request, if any. Should return the value last
	 * passed to {@link #setClient(Client)} (if on the server, within the current thread).
	 * @return the current client object in use
	 */
	@RemoteEndpointSupplier
	C getClient();

	/**
	 * Sets the current client to call back to. Called on the server to indicate that the given
	 * client instance is about to make a call. Called by the client to specify which client
	 * instance should have messages forwarded to it.
	 *
	 * @param client the client object to use on this server
	 */
	void setClient(C client);

	/**
	 * Closes the connection on the client. Should not be called in server code.
	 */
	void close();


	public static interface Connection {
		void data(String key, Object value);
		Object data(String key);

		void close();
	}
}

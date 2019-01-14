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

public abstract class AbstractServerImpl<S extends Server<S, C>, C extends Client<C, S>> extends RpcEndpoint<S, C> {
	/** In JSR-356, each server socket instance has exactly one client */
	private C client;

	protected AbstractServerImpl(EndpointImplConstructor<C> clientConstructor) {
		super(clientConstructor);
	}

//	@Override
	public void onOpen(Connection connection, C client) {
		// default empty implementation to allow clients to define this only if desired
	}

//	@Override
	public void onClose(Connection connection, C client) {
		// default empty implementation to allow clients to define this only if desired
	}

//	@Override
	public C getClient() {
		return client;
	}

//	@Override
	public void setClient(C client) {
		this.client = client;
	}

//	@Override
	public final void close() {
		throw new IllegalStateException("This method may not be called on the server, only on the client. To close the connection, invoke WebSocketConnection.close() on the connection you want to stop.");
	}
}

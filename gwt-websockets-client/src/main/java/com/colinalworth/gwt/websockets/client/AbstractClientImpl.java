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

import com.colinalworth.gwt.websockets.client.ConnectionClosedEvent.ConnectionClosedHandler;
import com.colinalworth.gwt.websockets.client.ConnectionClosedEvent.HasConnectionClosedHandlers;
import com.colinalworth.gwt.websockets.client.ConnectionOpenedEvent.ConnectionOpenedHandler;
import com.colinalworth.gwt.websockets.client.ConnectionOpenedEvent.HasConnectionOpenedHandlers;
import com.colinalworth.gwt.websockets.shared.Client;
import com.colinalworth.gwt.websockets.shared.Server;
import elemental2.dom.DomGlobal;
import org.gwtproject.event.shared.EventBus;
import org.gwtproject.event.shared.HandlerRegistration;
import org.gwtproject.event.shared.SimpleEventBus;

/**
 * Simple base class for client implementations that issues events on open and close.
 */
public abstract class AbstractClientImpl<C extends Client<C,S>, S extends Server<S,C>> implements Client<C, S>, HasConnectionOpenedHandlers, HasConnectionClosedHandlers {
	private final EventBus handlerManager = new SimpleEventBus();
	private S server;
	protected EventBus getHandlerManager() {
		return handlerManager;
	}
	
	@Override
	public void onOpen() {
		handlerManager.fireEventFromSource(new ConnectionOpenedEvent(), this);
	}

	@Override
	public void onClose() {
		handlerManager.fireEventFromSource(new ConnectionClosedEvent(), this);
	}

	@Override
	public void onError(Throwable error) {
		DomGlobal.console.log("An error occurred when handling a message sent to the client, override onError to handle this in your Client implementation", error);
	}

	@Override
	public HandlerRegistration addConnectionOpenedHandler(ConnectionOpenedHandler handler) {
		return handlerManager.addHandler(ConnectionOpenedEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration addConnectionClosedHandler(ConnectionClosedHandler handler) {
		return handlerManager.addHandler(ConnectionClosedEvent.getType(), handler);
	}

	@Override
	public void setServer(S server) {
		this.server = server;
	}

	@Override
	public S getServer() {
		return server;
	}
}

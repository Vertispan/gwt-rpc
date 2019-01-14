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
import org.gwtproject.event.shared.Event;
import org.gwtproject.event.shared.HandlerRegistration;

/**
 * Event fired to indicate that a WebSocket connection has closed for whatever reason,
 * allowing the client to attempt to re-open.
 * 
 * Note that there is currently no support to reuse a Server impl, a new one must be created.
 *
 */
public class ConnectionClosedEvent extends Event<ConnectionClosedHandler> {
	private static final Event.Type<ConnectionClosedHandler> TYPE = new Event.Type<ConnectionClosedEvent.ConnectionClosedHandler>();
	public static Event.Type<ConnectionClosedHandler> getType() {
		return TYPE;
	}

	@Override
	public Event.Type<ConnectionClosedHandler> getAssociatedType() {
		return getType();
	}

	@Override
	protected void dispatch(ConnectionClosedHandler handler) {
		handler.onConnectionClosed(this);
	}

	/**
	 * EventHandler interface for {@link ConnectionClosedEvent}.
	 *
	 */
	public interface ConnectionClosedHandler {
		void onConnectionClosed(ConnectionClosedEvent event);
	}

	/**
	 * Objects implementing this are advertising that they will fire {@link ConnectionClosedEvent}s
	 * to {@link ConnectionClosedHandler}s.
	 *
	 */
	public interface HasConnectionClosedHandlers {
		HandlerRegistration addConnectionClosedHandler(ConnectionClosedHandler handler);
	}
}

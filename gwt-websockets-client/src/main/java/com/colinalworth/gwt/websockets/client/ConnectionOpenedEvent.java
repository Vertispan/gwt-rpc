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

import com.colinalworth.gwt.websockets.client.ConnectionOpenedEvent.ConnectionOpenedHandler;
import org.gwtproject.event.shared.Event;
import org.gwtproject.event.shared.HandlerRegistration;

/**
 * Event fired to indicate that a WebSocket connection has been opened, and messages
 * may now be sent to the server.
 *
 */
public class ConnectionOpenedEvent extends Event<ConnectionOpenedHandler> {
	private static final Event.Type<ConnectionOpenedHandler> TYPE = new Event.Type<ConnectionOpenedHandler>();

	public static Event.Type<ConnectionOpenedHandler> getType() {
		return TYPE;
	}
	@Override
	public Event.Type<ConnectionOpenedHandler> getAssociatedType() {
		return getType();
	}

	@Override
	protected void dispatch(ConnectionOpenedHandler handler) {
		handler.onConnectionOpened(this);
	}

	/**
	 * EventHandler interface for {@link ConnectionOpenedEvent}.
	 *
	 */
	public interface ConnectionOpenedHandler {
		void onConnectionOpened(ConnectionOpenedEvent event);
	}

	/**
	 * Objects implementing this are advertising that they will fire {@link ConnectionOpenedEvent}s
	 * to {@link ConnectionOpenedHandler}s.
	 *
	 */
	public interface HasConnectionOpenedHandlers {
		HandlerRegistration addConnectionOpenedHandler(ConnectionOpenedHandler handler);
	}
}

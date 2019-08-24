/*
 * #%L
 * javaee-websocket-gwt-rpc-sample
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
package samples.easychatroom2.server;

import org.gwtproject.rpc.websockets.server.RemoteServiceServlet;
import org.gwtproject.rpc.websockets.shared.Callback;
import samples.easychatroom2.shared.ChatRemoteServiceAsync;
import samples.easychatroom2.shared.ChatRemoteServiceAsync_ImplRemote;

public class ChatRemoteServiceServlet extends RemoteServiceServlet<ChatRemoteServiceAsync> implements ChatRemoteServiceAsync {
	public ChatRemoteServiceServlet() {
		super(ChatRemoteServiceAsync_ImplRemote::new);
	}

	@Override
	public void send(String message, Callback<String, String> callback) {
		try {
			if (Math.random() > .7) {
				throw new IllegalStateException("Sample exception");
			}

			callback.onSuccess("Successfully sent " + message);
		} catch (Exception ex) {
			callback.onFailure("Error occurred: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
		}
	}
}

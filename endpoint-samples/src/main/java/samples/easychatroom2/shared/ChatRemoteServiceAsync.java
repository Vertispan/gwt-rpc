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
package samples.easychatroom2.shared;

import org.gwtproject.rpc.websockets.shared.Callback;
import org.gwtproject.rpc.websockets.shared.Endpoint;
import org.gwtproject.rpc.websockets.shared.Endpoint.NoRemoteEndpoint;
import org.gwtproject.rpc.websockets.shared.RemoteService.RemoteServiceAsync;

@Endpoint(value = NoRemoteEndpoint.class)
public interface ChatRemoteServiceAsync extends RemoteServiceAsync {
	void send(String message, Callback<String, Throwable> callback);
}

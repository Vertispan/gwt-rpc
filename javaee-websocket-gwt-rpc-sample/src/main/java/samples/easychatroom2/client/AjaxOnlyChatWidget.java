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
package samples.easychatroom2.client;

import com.colinalworth.gwt.websockets.client.RemoteServiceFactory;
import com.colinalworth.gwt.websockets.shared.Callback;
import com.colinalworth.gwt.websockets.shared.impl.ServiceDefTarget;
import elemental2.dom.DomGlobal;
import samples.easychatroom2.shared.ChatRemoteServiceAsync;
import samples.easychatroom2.shared.ChatRemoteServiceAsync_Impl;

public class AjaxOnlyChatWidget {

	public void onSendClicked() {
		ChatRemoteServiceAsync service = RemoteServiceFactory.create(ChatRemoteServiceAsync_Impl::new);
		((ServiceDefTarget) service).setServiceEntryPoint("/hello");
		service.send("foo", new Callback<String, Throwable>() {
			@Override
			public void onSuccess(String value) {
				DomGlobal.alert("success: " + value);
			}

			@Override
			public void onFailure(Throwable error) {
				DomGlobal.alert("failure" + error);
			}
		});
	}
}

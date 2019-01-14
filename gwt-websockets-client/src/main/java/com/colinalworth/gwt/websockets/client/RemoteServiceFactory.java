/*
 * #%L
 * gwt-websockets-client
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

import com.colinalworth.gwt.websockets.shared.RemoteService.RemoteServiceAsync;
import com.colinalworth.gwt.websockets.shared.impl.AbstractEndpointImpl.EndpointImplConstructor;
import com.colinalworth.gwt.websockets.shared.impl.ServiceDefTarget;
import com.vertispan.serial.streams.string.StringSerializationStreamReader;
import com.vertispan.serial.streams.string.StringSerializationStreamWriter;
import elemental2.dom.XMLHttpRequest;

import java.util.function.Consumer;

public final class RemoteServiceFactory {
	private RemoteServiceFactory() {

	}

	public static <T extends RemoteServiceAsync> T create(EndpointImplConstructor<T> constructor) {
		Consumer<String>[] responseHandler = new Consumer[1];
		T[] instance = (T[]) new RemoteServiceAsync[1];
		instance[0] = constructor.create(
				serializer -> {
					StringSerializationStreamWriter writer = new StringSerializationStreamWriter(serializer, "", "");
					writer.prepareToWrite();
					return writer;
				},
				stream -> {
					// just as we create a new writer stream for each call, we make a new xhr call as well
					XMLHttpRequest xmlHttpRequest = new XMLHttpRequest();

					String url = "";
					if (instance[0] instanceof ServiceDefTarget) {
						url = ((ServiceDefTarget) instance[0]).getServiceEntryPoint();
					}
					// user, pass?
					xmlHttpRequest.open("POST", url);

					// set headers (content type, etc)
					xmlHttpRequest.setRequestHeader("Content-Type", "text/x-gwt-rpc; charset=utf-8");

					// xsrf header, should find a convention that works for gwt3
					xmlHttpRequest.setRequestHeader("X-GWT-Permutation", "no-permutation");

					// set with creds

					xmlHttpRequest.onreadystatechange = e -> {
						if (xmlHttpRequest.readyState == 4/*DONE*/) {
							xmlHttpRequest.onreadystatechange = null;
							// call the consumer that we wired up for callbacks
							responseHandler[0].accept(xmlHttpRequest.responseText);
						}
						return null;
					};


					xmlHttpRequest.send(stream.toString());
				},
				(send, serializer) -> {
					responseHandler[0] = payload -> send.accept(new StringSerializationStreamReader(serializer, payload));
				}
		);
		return instance[0];
	}
}

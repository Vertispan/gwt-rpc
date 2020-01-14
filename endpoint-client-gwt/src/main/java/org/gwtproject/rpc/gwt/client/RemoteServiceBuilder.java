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
package org.gwtproject.rpc.gwt.client;

import java.util.HashSet;
import java.util.function.Consumer;

import org.gwtproject.rpc.api.RemoteService.RemoteServiceAsync;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl.EndpointImplConstructor;
import org.gwtproject.rpc.api.impl.AbstractRemoteServiceImpl;
import org.gwtproject.rpc.gwt.client.impl.RemoteServiceBuilderImpl;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.string.StringSerializationStreamWriter;

import elemental2.dom.XMLHttpRequest;
import elemental2.dom.XMLHttpRequest.OnerrorFn;
import elemental2.dom.XMLHttpRequest.OntimeoutFn;

/**
 * Builder interface for constructing a RemoteServiceAsync instance.
 */
public interface RemoteServiceBuilder<T extends RemoteServiceAsync> {

    /**
     * Sets user to be used for XMLHttpRequest connection.
     *
     * @param user
     * @return
     */
    RemoteServiceBuilder<T> setUser(String user);

    /**
     * Sets password to be used for XMLHttpRequest connection.
     *
     * @param password
     * @return
     */
    RemoteServiceBuilder<T> setPassword(String password);

    /**
	 * Sets url to be used for XMLHttpRequest connection.
	 *
	 * @param url
	 * @return
	 */
	RemoteServiceBuilder<T> setUrl(String url);

    /**
     * Sets method to be used for XMLHttpRequest messages.
     *
     * @param method
     * @return
     */
    RemoteServiceBuilder<T> setMethod(String method);

    /**
     * Sets timeout to be used for XMLHttpRequest messages.
     *
     * @param timeout
     * @return
     */
    RemoteServiceBuilder<T> setTimeout(int timeout);

	/**
	 * Sets a http header to be used for XMLHttpRequest messages.
	 *
	 * @param requestHeader
	 * @return
	 */
	RemoteServiceBuilder<T> setRequestHeader(HttpHeader requestHeader);

	/**
	 * Sets a callback for handling http responses with an error status.
	 * This would be any response code not in the 2xx range or a null payload.
	 *
	 * @param errorHandler the handler to send http errors to
	 * @return
	 */
	RemoteServiceBuilder<T> setHttpErrorHandler(HttpErrorHandler errorHandler);

    public interface HttpErrorHandler {
        public void onError(int statusCode, String statusText);
    }

	/**
	 * Specifies an onError handler for the underlying XMLHttpRequest.
	 * 
	 * @param errorHandler the handler to send error events to
	 */
    RemoteServiceBuilder<T> setOnErrorFn(OnerrorFn errorHandler);

	/**
     * Specifies an onTimeout handler for the underlying XMLHttpRequest.
     * 
     * @param timeoutHandler the handler to send timeout events to
     */
    RemoteServiceBuilder<T> setOnTimeoutFn(OntimeoutFn timeoutHandler);

	/**
	 * Creates a new instance of specified RemoteServiceAsync, using the settings provided by the setXXX methods.
	 *
	 * @return
	 */
	T build();

	/**
	 * Simple create method that takes the generated remote service's constructor and returns a functioning
	 * remote service builder.
	 */
    static <E extends RemoteServiceAsync> RemoteServiceBuilder<E> of(EndpointImplConstructor<E> constructor) {
        return new RemoteServiceBuilderImpl<E>() {
            @SuppressWarnings("unchecked")
            @Override
            public E build() {
                Consumer<String>[] responseHandler = new Consumer[1];
                E[] instance = (E[]) new RemoteServiceAsync[1];
                instance[0] = constructor.create(
                        serializer -> {
                            StringSerializationStreamWriter writer = new StringSerializationStreamWriter(serializer);
                            writer.prepareToWrite();
                            return writer;
                        },
                        stream -> {
                            // just as we create a new writer stream for each call, we make a new xhr call as well
                            XMLHttpRequest xmlHttpRequest = new XMLHttpRequest();
                            // Needs elemental 1.0.0
                            //xmlHttpRequest.timeout = getTimeout();

                            xmlHttpRequest.open(getMethod(), getUrl(), true, getUser(), getPassword());
                            HashSet<HttpHeader> headers = getRequestHeaders();
                            for (HttpHeader requestHeader : headers) {
                                if(requestHeader.getName() != null) {
                                    xmlHttpRequest.setRequestHeader(requestHeader.getName(), requestHeader.getValue());
                                }
                            }
                            
                            // endpoint+serializer checksums here
                            xmlHttpRequest.setRequestHeader("X-GWT-RPC-Checksum", ((AbstractRemoteServiceImpl<?>) instance[0]).getChecksum());
                            xmlHttpRequest.onerror = e -> getErrorHandler();
                            xmlHttpRequest.ontimeout = e -> getTimeoutHandler();
                            xmlHttpRequest.onreadystatechange = e -> {
                                if (xmlHttpRequest.readyState == 4/*DONE*/) {
                                    xmlHttpRequest.onreadystatechange = null;
		                            if(xmlHttpRequest.status >= 200 && xmlHttpRequest.status < 300 && xmlHttpRequest.responseText != null) {
		                            	// success
			                            // call the consumer that we wired up for callbacks
			                            responseHandler[0].accept(xmlHttpRequest.responseText);
		                            } else {
		                            	getHttpErrorHandler().onError(xmlHttpRequest.status, xmlHttpRequest.statusText);
		                            }
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
        };
    }

}

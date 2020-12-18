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

import elemental2.dom.DomGlobal;
import elemental2.dom.Event;
import elemental2.dom.ProgressEvent;
import elemental2.dom.XMLHttpRequest;

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
	RemoteServiceBuilder<T> setErrorHandler(ErrorHandler errorHandler);
	
	/**
	 * Base interface for errors occurring during an RPC call. 
	 */
	public interface ErrorEvent {
	}
	
    /**
     * This event is thrown on an unsuccessful http request where the code 
     * is not in 2xx range, or where the response text is empty such that 
     * there is nothing to deserialize. 
     */
	public class HttpErrorEvent implements ErrorEvent {
	    private int statusCode;
	    private String statusText;
	    private String responseText;
        
	    public HttpErrorEvent(int statusCode, String statusText, String responseText) {
            super();
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.responseText = responseText;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusText() {
            return statusText;
        }
        
        public String getResponseText() {
            return responseText;
        }

        @Override
        public String toString() {
            return "HttpErrorEvent [statusCode=" + statusCode + ", statusText=" + statusText + ", hasResponseText=" + (responseText == null) + "]";
        }
        
	}

	/**
	 * Wraps a native XMLHttpRequest.onError event. 
	 */
	public class GeneralErrorEvent implements ErrorEvent {
	    private Event nativeEvent;
	    
        public GeneralErrorEvent(Event nativeEvent) {
            this.nativeEvent = nativeEvent;
        }

        public Event getNativeEvent() {
            return nativeEvent;
        }

        @Override
        public String toString() {
            return "GeneralErrorEvent [nativeEvent=" + nativeEvent + "]";
        }
	}

    /**
     * Wraps a native XMLHttpRequest.onTimeout event. 
     */
    public class TimeoutEvent implements ErrorEvent {
        private ProgressEvent nativeEvent;
        
        public TimeoutEvent(ProgressEvent nativeEvent) {
            this.nativeEvent = nativeEvent;
        }

        public ProgressEvent getNativeEvent() {
            return nativeEvent;
        }

        @Override
        public String toString() {
            return "TimeoutEvent [nativeEvent=" + nativeEvent + "]";
        }
    }

    /**
     * Handler for error events that occur during an RPC call.
     */
    public interface ErrorHandler {
        public void onError(ErrorEvent errorEvent);
    }

    /**
     * Sets a http header to be used for XMLHttpRequest messages.
     *
     * @param requestHeader
     * @return
     */
    RemoteServiceBuilder<T> setRequestCallback(RequestCallback requestCallBack);

    /**
     * Callback before the RPC request is sent. This can be used to 
     * modify the XMLHttpRequest and provide custom handlers.
     *  
     */
    public interface RequestCallback {
        /**
         * <p>The responseHandler can be used if a custom onreadystatechange is required. Once data 
         * has been received merely add this call to handle the deserialization (be sure to handle null responseText first):</p>
         * <pre>
         * <code>
         *      responseHandler.accept(xmlHttpRequest.responseText);
         * </code>
         * </pre> 
         * 
         * @param xmlHttpRequest the native XMLHttpRequest
         * @param responseHandler the callback that handles the deserialization
         */
        public void beforeSend(XMLHttpRequest xmlHttpRequest, Consumer<String> responseHandler);
    }

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
                            if(getErrorHandler() == null) {
                                setErrorHandler(e -> {
                                    DomGlobal.console.log("An error occurred - pass a error handler to your service builder to handle this yourself", e);
                                });
                            }
                            // endpoint+serializer checksums here
                            xmlHttpRequest.setRequestHeader("X-GWT-RPC-Checksum", ((AbstractRemoteServiceImpl<?>) instance[0]).getChecksum());
                            xmlHttpRequest.onerror = e -> { getErrorHandler().onError(new GeneralErrorEvent(e)); return null; };
                            xmlHttpRequest.ontimeout = e -> { getErrorHandler().onError(new TimeoutEvent(e)); };
                            xmlHttpRequest.onreadystatechange = e -> {
                                if (xmlHttpRequest.readyState == 4/*DONE*/) {
                                    xmlHttpRequest.onreadystatechange = null;
		                            if(xmlHttpRequest.status >= 200 && xmlHttpRequest.status < 300 && xmlHttpRequest.responseText != null) {
		                            	// success
			                            // call the consumer that we wired up for callbacks
			                            responseHandler[0].accept(xmlHttpRequest.responseText);
		                            } else {
		                            	getErrorHandler().onError(new HttpErrorEvent(xmlHttpRequest.status, xmlHttpRequest.statusText, xmlHttpRequest.responseText));
		                            }
                                }
                                return null;
                            };
                            
                            if(getRequestCallback() != null) {
                                getRequestCallback().beforeSend(xmlHttpRequest, responseHandler[0]);
                            }
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

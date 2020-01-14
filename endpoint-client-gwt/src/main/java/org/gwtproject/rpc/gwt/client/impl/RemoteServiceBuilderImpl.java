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
package org.gwtproject.rpc.gwt.client.impl;

import java.util.HashSet;

import org.gwtproject.rpc.api.RemoteService.RemoteServiceAsync;
import org.gwtproject.rpc.gwt.client.HttpHeader;
import org.gwtproject.rpc.gwt.client.RemoteServiceBuilder;

import elemental2.dom.DomGlobal;
import elemental2.dom.XMLHttpRequest;
import elemental2.dom.XMLHttpRequest.OnerrorFn;
import elemental2.dom.XMLHttpRequest.OntimeoutFn;

public abstract class RemoteServiceBuilderImpl<T extends RemoteServiceAsync> implements RemoteServiceBuilder<T> {
    public static final String RPC_CONTENT_TYPE = "text/x-gwt-rpc; charset=utf-8";
    private String user;
    private String password;
    private String url;
    private String method;
    private int timeout;
    private HashSet<HttpHeader> requestHeaders = new HashSet<>();
    private HttpErrorHandler httpErrorHandler;
    private XMLHttpRequest.OnerrorFn errorHandler;
    private XMLHttpRequest.OntimeoutFn timeoutHandler;

    public RemoteServiceBuilderImpl() {
        method = "POST";
        url = "";
		httpErrorHandler = (statusCode, statusText) -> {
			DomGlobal.console.log("RPC request error: statusCode=" + statusCode + ", statusText:" + statusText
					+ " - To handle this add an HttpErrorHandler to the RemoteServiceBuilder instance.");
		};
        errorHandler = e -> {
            DomGlobal.console.log("OnError: " + e
                    + " - To handle this add an OnErrorHandler to the RemoteServiceBuilder instance.");
            return null;
        };
        timeoutHandler = e -> {
            DomGlobal.console.log("OnTimeout: " + e
                    + " - To handle this add an OnTimeoutHandler to the RemoteServiceBuilder instance.");
        };
    }
    
    @Override
    public RemoteServiceBuilder<T> setUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    public RemoteServiceBuilder<T> setUser(String user) {
        this.user = user;
        return this;

    }

    @Override
    public RemoteServiceBuilder<T> setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public RemoteServiceBuilder<T> setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public RemoteServiceBuilder<T> setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public RemoteServiceBuilder<T> setRequestHeader(HttpHeader requestHeader) {
        requestHeaders.add(requestHeader);
        return this;
    }
    
    @Override
    public RemoteServiceBuilder<T> setHttpErrorHandler(HttpErrorHandler rpcErrorCallback) {
    	this.httpErrorHandler = rpcErrorCallback;
    	return this;
    }

    @Override
    public RemoteServiceBuilder<T> setOnErrorFn(OnerrorFn errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }
    
    @Override
    public RemoteServiceBuilder<T> setOnTimeoutFn(OntimeoutFn timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
        return null;
    }
    
    public String getUser() {
        return user;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getUrl() {
        return url;
    }
    
    public String getMethod() {
        return method;
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public HashSet<HttpHeader> getRequestHeaders() {
        return requestHeaders;
    }

    public HttpErrorHandler getHttpErrorHandler() {
		return httpErrorHandler;
	}
    
    public OnerrorFn getErrorHandler() {
        return errorHandler;
    }

    public OntimeoutFn getTimeoutHandler() {
        return timeoutHandler;
    }

}
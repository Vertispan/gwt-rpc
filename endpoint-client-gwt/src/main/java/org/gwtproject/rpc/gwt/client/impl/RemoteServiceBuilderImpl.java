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

public abstract class RemoteServiceBuilderImpl<T extends RemoteServiceAsync> implements RemoteServiceBuilder<T> {
    public static final String RPC_CONTENT_TYPE = "text/x-gwt-rpc; charset=utf-8";
    private String user;
    private String password;
    private String url;
    private String method;
    private int timeout;
    private HashSet<HttpHeader> requestHeaders = new HashSet<>();
    private ErrorHandler errorHandler;
    private RequestCallback requestCallback;

    public RemoteServiceBuilderImpl() {
        method = "POST";
        url = "";
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
    public RemoteServiceBuilder<T> setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public RemoteServiceBuilder<T> setRequestCallback(RequestCallback requestCallBack) {
        this.requestCallback = requestCallBack;
        return this;
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

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public RequestCallback getRequestCallback() {
        return requestCallback;
    }
}
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

import elemental2.dom.DomGlobal;
import org.gwtproject.rpc.api.Server;
import org.gwtproject.rpc.gwt.client.ServerBuilder;

public abstract class ServerBuilderImpl<S extends Server<? super S, ?>> implements ServerBuilder<S> {
	private String url;
	private URL urlBuilder = new URL(DomGlobal.self.location.href);
	private ConnectionErrorHandler errorHandler;
	private String subProtocols;

	/**
	 *
	 */
	public ServerBuilderImpl() {
		urlBuilder.protocol = DomGlobal.self.location.protocol.equals("https") ? "wss": "ws";
	}

	@Override
	public void setConnectionErrorHandler(ConnectionErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}
	public ConnectionErrorHandler getErrorHandler() {
		return errorHandler;
	}

	@Override
	public ServerBuilder<S> setUrl(String url) {
		this.url = url;
		return this;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url == null ? urlBuilder.toString_() : url;
	}

   /**
     * @return the websocket sub protocols
     */
    public String getSubProtocols() {
        return subProtocols;
    }

	@Override
	public ServerBuilder<S> setProtocol(String protocol) {
		urlBuilder.protocol = protocol;
		return this;
	}
	@Override
	public ServerBuilder<S> setHostname(String hostname) {
		urlBuilder.host = hostname;
		return this;
	}
	@Override
	public ServerBuilder<S> setPort(int port) {
		urlBuilder.port = "" + port;
		return this;
	}
	@Override
	public ServerBuilder<S> setPath(String path) {
		urlBuilder.pathname = path;
		return this;
	}
    @Override
    public ServerBuilder<S> setSubProtocols(String subProtocols) {
        this.subProtocols = subProtocols;
        return this;
    }	
}
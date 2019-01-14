/*
 * #%L
 * gwt-websockets-api
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
package com.colinalworth.gwt.websockets.shared;

import com.colinalworth.gwt.websockets.shared.Endpoint.BaseClass;
import com.colinalworth.gwt.websockets.shared.Endpoint.NoRemoteEndpoint;
import com.colinalworth.gwt.websockets.shared.Endpoint.RemoteEndpointSupplier;
import com.colinalworth.gwt.websockets.shared.impl.AbstractRemoteServiceImpl;

/**
 * GWT2 RPC-like annotation, to be used instead of extending RemoteService.
 *
 * If an Async interface is provided it will be used, otherwise one will be
 * generated automatically. That interface should extend RemoteServiceAsync
 * and be annotated with @Endpoint.
 */
public @interface RemoteService {


	@BaseClass(AbstractRemoteServiceImpl.class)
	public interface RemoteServiceAsync {

		/**
		 * Hint to processor that this doesn't actually have a real remote, and it should just build
		 * a stub for serialization purposes.
		 *
		 * Do not call this method or implement it.
		 */
		@RemoteEndpointSupplier
		default NoRemoteEndpoint<?> noRemoteEndpoint() {
			return null;
		}
	}
}

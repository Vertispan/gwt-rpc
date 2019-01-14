/*
 * #%L
 * workers
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
package com.colinalworth.gwt.worker.client;

import com.colinalworth.gwt.websockets.shared.Endpoint;
import com.colinalworth.gwt.websockets.shared.Endpoint.BaseClass;
import com.colinalworth.gwt.worker.client.impl.AbstractWorkerEndpointImpl;

@BaseClass(AbstractWorkerEndpointImpl.class)
public interface MessagePortEndpoint<E> {
	void setRemote(E remote);

	@Endpoint.RemoteEndpointSupplier
	E getRemote();
}

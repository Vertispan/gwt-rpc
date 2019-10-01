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
package org.gwtproject.rpc.worker.client;

import org.gwtproject.rpc.api.Endpoint;
import org.gwtproject.rpc.api.Endpoint.BaseClass;
import org.gwtproject.rpc.worker.client.impl.AbstractWorkerEndpointImpl;

@BaseClass(AbstractWorkerEndpointImpl.class)
public interface MessagePortEndpoint<E> {
	void setRemote(E remote);

	@Endpoint.RemoteEndpointSupplier
	E getRemote();

	/**
	 * Called when an error occurs while handling a message in one of the other client methods. If a
	 * ConnectionErrorHandler is provided to the server builder, that will be used in handling
	 * serialization/deserialization and connection errors.
	 * @param error the error that occurred
	 */
	void onError(Throwable error);
}

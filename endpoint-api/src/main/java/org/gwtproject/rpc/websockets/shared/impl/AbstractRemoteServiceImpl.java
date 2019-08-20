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
package org.gwtproject.rpc.websockets.shared.impl;

import org.gwtproject.rpc.websockets.shared.Endpoint.NoRemoteEndpoint;
import org.gwtproject.rpc.serialization.api.SerializationStreamReader;
import org.gwtproject.rpc.serialization.api.SerializationStreamWriter;
import org.gwtproject.rpc.serialization.api.TypeSerializer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractRemoteServiceImpl<R extends NoRemoteEndpoint<?>>
		extends AbstractEndpointImpl implements ServiceDefTarget {
	private R server;
	private String serviceEntryPoint;

	protected <W extends SerializationStreamWriter> AbstractRemoteServiceImpl(
			Function<TypeSerializer, W> writerFactory,
			Consumer<W> send,
			TypeSerializer serializer,
			BiConsumer<Consumer<SerializationStreamReader>, TypeSerializer> onMessage
	) {
		super(writerFactory, send, serializer, onMessage);
	}

	public void setRemote(R remote) {
		this.server = remote;
	}

	public R getRemote() {
		return server;
	}

	public abstract String getChecksum();

	@Override
	public String getSerializationPolicyName() {
		throw new UnsupportedOperationException("getSerializationPolicyName is not supported, use getChecksum instead");
	}

	@Override
	public String getServiceEntryPoint() {
		return serviceEntryPoint;
	}

	@Override
	public void setServiceEntryPoint(String serviceEntryPoint) {
		this.serviceEntryPoint = serviceEntryPoint;
	}
}

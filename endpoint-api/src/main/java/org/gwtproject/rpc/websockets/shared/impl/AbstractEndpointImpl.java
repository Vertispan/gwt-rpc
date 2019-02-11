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

import com.google.gwt.user.client.rpc.SerializationException;
import org.gwtproject.rpc.serialization.api.SerializationStreamReader;
import org.gwtproject.rpc.serialization.api.SerializationStreamWriter;
import org.gwtproject.rpc.serialization.api.TypeSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for use in implementing any kind of endpoint, simplifying the code required to have in
 * generated concrete implementations.
 */
public abstract class AbstractEndpointImpl {
	/**
	 * Interface describing the constructor of any concrete subclass for use in wrapping these instances.
	 */
	@FunctionalInterface
	public interface EndpointImplConstructor<E> {
		<W extends SerializationStreamWriter> E create(
				Function<TypeSerializer, W> writerFactory,
				Consumer<W> send,
				BiConsumer<Consumer<SerializationStreamReader>, TypeSerializer> onMessage
		);
	}

	private final Function<TypeSerializer, SerializationStreamWriter> writerFactory;
	private final Consumer<SerializationStreamWriter> send;

	private final TypeSerializer serializer;

	// count starts at 1, leaving zero for remote methods
	private int nextCallbackId = 1;
	private Map<Integer, ReadingCallback<?,?>> callbacks = new HashMap<>();


	protected <W extends SerializationStreamWriter> AbstractEndpointImpl(
			Function<TypeSerializer, W> writerFactory,
			Consumer<W> send,
			TypeSerializer serializer,
			BiConsumer<Consumer<SerializationStreamReader>, TypeSerializer> onMessage) {
		this.writerFactory = (Function<TypeSerializer, SerializationStreamWriter>) writerFactory;
		this.send = (Consumer<SerializationStreamWriter>) send;
		this.serializer = serializer;
		onMessage.accept(this::__onMessage, serializer);
	}

	/**
	 * Push any errors to the local (non-generated) endpoint implementation.
	 */
	protected abstract void __onError(Throwable ex);

	/**
	 * Invoke the remote invoked method on the local (non-generated) endpoint implementation.
	 */
	protected abstract void __invoke(int recipient, SerializationStreamReader reader) throws SerializationException;

	public void __onMessage(SerializationStreamReader reader) {
		try {
			//thrown away, but they are still in the payoad for now
			String moduleBaseURL = reader.readString();
			String spsn = reader.readString();

			int recipient = reader.readInt();
			if (recipient >= 0) {
				__invoke(recipient, reader);
			} else {
				ReadingCallback<?, ?> callback = callbacks.get(-recipient);
				callback.handle(reader);
			}
		} catch (SerializationException ex) {
			__onError(ex);
		}
	}

	private SerializationStreamWriter __startCall() {
		return writerFactory.apply(serializer);
	}
	private void __endCall(SerializationStreamWriter writer) {
		send.accept(writer);
	}

	/**
	 * Easy lambda to let generated classes run code which might throw within __send()
	 */
	protected interface Send {
		void send(SerializationStreamWriter writer) throws SerializationException;
	}

	protected void __send(int recipient, Send s) {
		SerializationStreamWriter writer = __startCall();
		try {
			writer.writeInt(recipient);
			s.send(writer);
			__endCall(writer);
		} catch (SerializationException e) {
			__onError(e);
			throw new RuntimeException(e);
		}
	}
	protected void __send(int recipient, Send s, ReadingCallback<?, ?> callback) {
		SerializationStreamWriter writer = __startCall();
		try {
			writer.writeInt(recipient);

			// add the callbackId to the message to send so the remote end knows it will need a callback
			// object when handling the rest of the body
			int callbackId = nextCallbackId++;
			writer.writeInt(callbackId);
			s.send(writer);

			__endCall(writer);
			//only after we've successfully sent, register the callback
			callbacks.put(callbackId, callback);
		} catch (SerializationException e) {
			//TODO report? can't actually pass to Callback.onFailure, since it might expect something else
			throw new RuntimeException(e);
		}
	}

	protected static abstract class ReadingCallback<T, F> {
		public final void handle(SerializationStreamReader reader) throws SerializationException {
			boolean success = reader.readBoolean();
			if (success) {
				success(reader);
			} else {
				failure(reader);
			}
		}
		public abstract void success(SerializationStreamReader reader) throws org.gwtproject.rpc.serialization.api.SerializationException;
		public abstract void failure(SerializationStreamReader reader) throws org.gwtproject.rpc.serialization.api.SerializationException;
	}
}

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

import elemental2.core.ArrayBuffer;
import elemental2.core.JsArray;
import elemental2.dom.MessagePort;
import jsinterop.base.Any;
import jsinterop.base.Js;
import org.gwtproject.nio.HasArrayBufferView;
import org.gwtproject.nio.TypedArrayHelper;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl.EndpointImplConstructor;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;
import org.gwtproject.rpc.worker.client.impl.AbstractWorkerFactoryImpl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * todo how to differentiate between shared, dedicated, service workers...
 */
public interface WorkerFactory<R extends MessagePortEndpoint<L>, L extends MessagePortEndpoint<R>> {

	/**
	 * Creates a worker with the remote JS file specified, connected to the local endpoint.
	 *
	 * @param pathToJs the path to the JS file which will describe this worker
	 * @param local the local interface which the new remote worker can send messages to
	 * @return the newly created worker, which will still be starting up.
	 */
	R createDedicatedWorker(String pathToJs, L local);

	/**
	 * Creates or reuses a shared worker with the remote JS file specified, connected to this
	 * local endpoint.
	 *
	 * @param pathToJs the path to the JS file which will describe this worker
	 * @param local the local interface which the remote worker can send messages to
	 * @return the new or existing worker
	 */
	R createSharedWorker(String pathToJs, L local);

	/**
	 * Wraps a MessagePort as an endpoint, permitting communicate with other pages or workers,
	 * controlled by application code.
	 * @param remote the message port that is able to communicate with the remote instance
	 * @param local the local interface which the remote endpoint can send messages to
	 * @return the rpc interface, wrapped around a message port
	 */
	R wrapRemoteMessagePort(MessagePort remote, L local);

	/**
	 * From within a dedicated worker, wraps the current global scope to communicate with the calling
	 * host page.
	 * @param local the local interface which the remote page can send messages to
	 * @return the remote rpc interfaces, wrapped around the remote page
	 */
	R wrapDedicatedWorkerGlobalScope(L local);

	/**
	 * Given a constructor for an endpoint, creates a WorkerFactory to be able to initiate communicate with that endpoint.
	 * @param constructor the constructor function, typically looks something like {@code MyRpcInterface_Impl::new}.
	 * @param <R> the remote interface's type
	 * @param <L> the local interface's type
	 * @return a worker factory that is able to initiate communicate with this type of endpoint
	 */
	static <R extends MessagePortEndpoint<L>, L extends MessagePortEndpoint<R>> WorkerFactory<R, L> of(EndpointImplConstructor<R> constructor) {
		return new AbstractWorkerFactoryImpl<R, L>() {
			@Override
			protected R create(EmitsMessages emitsMessages, PostMessage postMessage) {
				return constructor.create(
						ByteBufferSerializationStreamWriter::new,
						stream -> {
							JsArray<String> stringTable = JsArray.asJsArray(stream.getFinishedStringTable()).slice();
							ArrayBuffer payload = Js.cast(((HasArrayBufferView) stream.getPayloadBytes()).getTypedArray().buffer);

							postMessage.send(new JsArray<>(payload, stringTable), new JsArray<>(payload));
						},
						(send, serializer) -> {
							emitsMessages.setOnmessage( message -> {
								JsArray<Any> data = Js.cast(message.data);
								ByteBuffer byteBuffer = TypedArrayHelper.wrap(data.getAt(0).cast());
								byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
								String[] strings = data.getAt(1).<JsArray<String>>cast().asArray(new String[0]);
								send.accept(new ByteBufferSerializationStreamReader(serializer, byteBuffer, strings));
							});
						}

				);
			}
		};
	}
}

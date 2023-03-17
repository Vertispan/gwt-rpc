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

import org.gwtproject.nio.HasArrayBufferView;
import org.gwtproject.nio.TypedArrayHelper;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamReader;
import org.gwtproject.rpc.serialization.stream.bytebuffer.ByteBufferSerializationStreamWriter;
import org.gwtproject.rpc.api.impl.AbstractEndpointImpl.EndpointImplConstructor;
import org.gwtproject.rpc.worker.client.impl.AbstractWorkerFactoryImpl;
import org.gwtproject.rpc.worker.client.worker.MessagePort;
import elemental2.core.ArrayBuffer;
import elemental2.core.JsArray;
import elemental2.core.JsString;
import jsinterop.base.Any;
import jsinterop.base.Js;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * todo how to differentiate between shared, dedicated, service workers...
 */
public interface WorkerFactory<R extends MessagePortEndpoint<L>, L extends MessagePortEndpoint<R>> {

	/**
	 * Creates a worker with the remote JS connected to the local endpoint.
	 * @param pathToJs the path to the JS file which will describe this worker
	 * @param local the local interface which the new remote worker can send messages to
	 * @return the newly created worker, which will still be starting up.
	 */
	R createDedicatedWorker(String pathToJs, L local);

	R createSharedWorker(String pathToJs, L local);

	R wrapRemoteMessagePort(MessagePort remote, L local);

	static <R extends MessagePortEndpoint<L>, L extends MessagePortEndpoint<R>> WorkerFactory<R, L>
	of(EndpointImplConstructor<R> constructor) {
		return new AbstractWorkerFactoryImpl<R, L>() {
			@Override
			protected R create(MessagePort worker) {
				worker.start();
				return constructor.create(
						ByteBufferSerializationStreamWriter::new,
						stream -> {
							String[] stringTable = stream.getFinishedStringTable();
							ArrayBuffer payload = Js.cast(((HasArrayBufferView) stream.getPayloadBytes()).getTypedArray().buffer);

							worker.postMessage(new JsArray<>(payload, stringTable), new JsArray<>(payload));
						},
						(send, serializer) -> {
							worker.addMessageHandler(message -> {
								JsArray<Any> data = message.getData();
								ByteBuffer byteBuffer = TypedArrayHelper.wrap(data.getAt(0).cast());
								byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
								String[] strings = data.getAt(1).cast();
								send.accept(new ByteBufferSerializationStreamReader(serializer, byteBuffer, strings));
							});
						}

				);
			}
		};
	}
}

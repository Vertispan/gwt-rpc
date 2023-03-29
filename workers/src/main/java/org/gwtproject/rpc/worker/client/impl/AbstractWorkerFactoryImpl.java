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
package org.gwtproject.rpc.worker.client.impl;

import elemental2.core.JsArray;
import elemental2.core.Transferable;
import elemental2.dom.DedicatedWorkerGlobalScope;
import elemental2.dom.DomGlobal;
import elemental2.dom.MessagePort;
import elemental2.dom.SharedWorker;
import elemental2.dom.Worker;
import jsinterop.annotations.JsFunction;
import org.gwtproject.rpc.worker.client.MessagePortEndpoint;
import org.gwtproject.rpc.worker.client.WorkerFactory;

/**
 * base class for generated factories, with a hook to create the remote endpoint to connect to
 */
public abstract class AbstractWorkerFactoryImpl<R extends MessagePortEndpoint<L>, L extends MessagePortEndpoint<R>> implements WorkerFactory<R, L> {

	@Override
	public R createDedicatedWorker(String pathToJs, L local) {
		Worker worker = new Worker(pathToJs);

		R remote = create(fn -> worker.onmessage = fn::onInvoke, worker::postMessage);

		remote.setRemote(local);
		local.setRemote(remote);

		return remote;
	}

	@Override
	public R createSharedWorker(String pathToJs, L local) {
		SharedWorker worker = new SharedWorker(pathToJs, pathToJs);
		return wrapRemoteMessagePort(worker.port, local);
	}

	@Override
	public R wrapRemoteMessagePort(MessagePort remote, L local) {
		R r = create(fn -> remote.onmessage = fn::onInvoke, remote::postMessage);

		r.setRemote(local);
		local.setRemote(r);

		return r;
	}

	@Override
	public R wrapDedicatedWorkerGlobalScope(L local) {
		DedicatedWorkerGlobalScope scope = (DedicatedWorkerGlobalScope) DomGlobal.self;
		R r = create(scope::setOnmessage, scope::postMessage);

		r.setRemote(local);
		local.setRemote(r);

		return null;
	}
	@JsFunction
	public interface EmitsMessages {
		void setOnmessage(DedicatedWorkerGlobalScope.OnmessageFn onmessage);
	}
	@JsFunction
	public interface PostMessage {
		void send(Object message, JsArray<Transferable> transfer);
	}
	/**
	 * Build the actual instance to connect to.
	 */
	protected abstract R create(EmitsMessages emitsMessages, PostMessage postMessage);
}

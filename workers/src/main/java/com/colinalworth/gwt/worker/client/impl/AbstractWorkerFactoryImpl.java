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
package com.colinalworth.gwt.worker.client.impl;

import com.colinalworth.gwt.worker.client.MessagePortEndpoint;
import com.colinalworth.gwt.worker.client.WorkerFactory;
import com.colinalworth.gwt.worker.client.worker.MessagePort;
import com.colinalworth.gwt.worker.client.worker.SharedWorker;
import com.colinalworth.gwt.worker.client.worker.Worker;

/**
 * base class for generated factories, with a hook to create the remote endpoint to connect to
 */
public abstract class AbstractWorkerFactoryImpl<R extends MessagePortEndpoint<L>, L extends MessagePortEndpoint<R>> implements WorkerFactory<R, L> {

	@Override
	public R createDedicatedWorker(String pathToJs, L local) {

		Worker worker = new Worker(pathToJs);

		R remote = create(worker);

		remote.setRemote(local);
		local.setRemote(remote);

		return remote;
	}

	@Override
	public R createSharedWorker(String pathToJs, L local) {
		SharedWorker worker = new SharedWorker(pathToJs, pathToJs);
		R remote = create(worker.getPort());

		remote.setRemote(local);
		local.setRemote(remote);

		return remote;
	}

	@Override
	public R wrapRemoteMessagePort(MessagePort remote, L local) {
		R r = create(remote);

		r.setRemote(local);
		local.setRemote(r);

		return r;
	}

	/**
	 * build the actual instance to connect
	 */
	protected abstract R create(MessagePort worker);
}

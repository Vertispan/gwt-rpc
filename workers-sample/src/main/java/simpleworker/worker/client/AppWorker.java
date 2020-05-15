/*
 * #%L
 * workers-sample
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
package simpleworker.worker.client;

import elemental2.dom.DomGlobal;
import org.gwtproject.rpc.api.Callback;
import org.gwtproject.rpc.worker.client.WorkerFactory;
import org.gwtproject.rpc.worker.client.worker.MessagePort;
import com.google.gwt.core.client.EntryPoint;
import simpleworker.common.client.MyHost;
import simpleworker.common.client.MyHost_Impl;
import simpleworker.common.client.MyWorker;

/**
 * Created by colin on 1/21/16.
 */
public class AppWorker implements EntryPoint {

	@Override
	public void onModuleLoad() {
		DomGlobal.console.log("Loaded module AppWorker");

		WorkerFactory<MyHost, MyWorker> factory = WorkerFactory.of(MyHost_Impl::new);

		factory.wrapRemoteMessagePort(self(), new MyWorker() {
			@Override
			public void onError(Throwable error) {
				DomGlobal.console.log("An error occurred", error);
			}

			@Override
			public void ping() {
				getRemote().pong();
			}

			@Override
			public void split(String pattern, String input, Callback<String[], String> callback) {
				try {
					callback.onSuccess(input.split(pattern));
				} catch (Exception e) {
					callback.onFailure(e.getMessage() == null ? e.toString() : e.getMessage());
				}
			}

			private MyHost remote;

			@Override
			public void setRemote(MyHost myHost) {
				remote = myHost;
			}

			@Override
			public MyHost getRemote() {
				return remote;
			}
		});
	}

	private native MessagePort self() /*-{
		return $wnd;
	}-*/;

}

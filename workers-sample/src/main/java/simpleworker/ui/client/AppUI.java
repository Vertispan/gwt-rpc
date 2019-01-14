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
package simpleworker.ui.client;

import com.colinalworth.gwt.websockets.shared.Callback;
import com.colinalworth.gwt.worker.client.WorkerFactory;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;
import simpleworker.common.client.MyHost;
import simpleworker.common.client.MyWorker;
import simpleworker.common.client.MyWorker_Impl;

public class AppUI implements EntryPoint {

	@Override
	public void onModuleLoad() {
		WorkerFactory<MyWorker, MyHost> factory = WorkerFactory.of(MyWorker_Impl::new);

		final MyWorker worker = factory.createDedicatedWorker(GWT.getModuleBaseForStaticFiles() + "../simpleworker_worker/worker.js", new MyHost() {
			private MyWorker remote;

			@Override
			public void setRemote(MyWorker myWorker) {
				this.remote = myWorker;
			}

			@Override
			public MyWorker getRemote() {
				return remote;
			}

			@Override
			public void pong() {
				Window.alert("pong");
			}
		});


		RootPanel.get().add(new Button("Ping", new ClickHandler() {
			@Override
			public void onClick(ClickEvent clickEvent) {
				worker.ping();
			}
		}));
		RootPanel.get().add(new Button("Split", new ClickHandler() {
			@Override
			public void onClick(ClickEvent clickEvent) {
				String result = Window.prompt("Split this text on \",\"", "a,b,c,d");
				if (result != null) {
					worker.split(",", result, new Callback<String[], Throwable>() {
						@Override
						public void onFailure(Throwable throwable) {
							Window.alert("failure: " + throwable.getMessage());
						}
						@Override
						public void onSuccess(String[] strings) {
							Window.alert(strings.length + " items");
						}
					});
				}
			}
		}));
	}

}

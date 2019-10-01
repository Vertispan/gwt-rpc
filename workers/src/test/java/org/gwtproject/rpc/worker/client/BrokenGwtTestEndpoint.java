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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Created by colin on 1/14/16.
 */
public class BrokenGwtTestEndpoint extends GWTTestCase {
	@Override
	public String getModuleName() {
		return "org.gwtproject.rpc.worker.RpcToWorkers";
	}

//	@Endpoint
//	public interface MyHost extends MessagePortEndpoint<MyWorker> {
//		void ping();
//	}
//	@Endpoint
//	public interface MyWorker extends MessagePortEndpoint<MyHost> {
//		void pong();
//
//		void split(String input, String pattern, Callback<ArrayList<String>, Throwable> callback);
//	}
//
//	public void testSimpleEndpoint() {
//		delayTestFinish(1000);
//
//		MyWorker worker = WorkerFactory.of(MyWorker_Impl::new).createDedicatedWorker("simpleWorker.js", new MyHost() {
//			@Override
//			public void ping() {
//				remote.pong();
//			}
//
//			private MyWorker remote;
//			@Override
//			public void setRemote(MyWorker myWorker) {
//				remote = myWorker;
//				remote.split("a,b,c", ",", new Callback<ArrayList<String>, Throwable>() {
//					@Override
//					public void onFailure(Throwable throwable) {
//						fail(throwable.getMessage());
//					}
//
//					@Override
//					public void onSuccess(ArrayList<String> strings) {
//						List<String> expected = new ArrayList<String>();
//						expected.add("a");
//						expected.add("b");
//						expected.add("c");
//						assertEquals(expected, strings);
//						finishTest();
//					}
//				});
//			}
//
//			@Override
//			public MyWorker getRemote() {
//				return remote;
//			}
//		});
//	}
}
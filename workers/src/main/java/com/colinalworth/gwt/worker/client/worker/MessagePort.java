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
package com.colinalworth.gwt.worker.client.worker;

import com.colinalworth.gwt.worker.client.worker.MessageEvent.MessageHandler;
import com.google.gwt.typedarrays.shared.ArrayBuffer;
import elemental2.core.JsArray;
import elemental2.core.Transferable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Created by colin on 1/18/16.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public class MessagePort {

	public final native void postMessage(Object jso, JsArray<? extends Transferable> buffers);

	public native void start();

	public native void close();

	@JsOverlay
	public final void addMessageHandler(MessageHandler handler) {
		addEventListener("message", handler);
	}

	public native void addEventListener(String type, MessageHandler listener);

}

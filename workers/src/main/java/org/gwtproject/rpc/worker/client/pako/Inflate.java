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
package org.gwtproject.rpc.worker.client.pako;

import elemental2.core.ArrayBuffer;
import elemental2.core.ArrayBufferView;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * JsInterop wrapper for nodeca/pako, "zlib port to javascript, very fast!"
 *
 * Not presently used, but could serve as an implementation to compress ByteBuffers
 * when serializing for websockets.
 */
@JsType(isNative = true, name = "pako.Inflate")
public class Inflate {
	public native boolean push(ArrayBuffer array, boolean last);
	public native boolean push(ArrayBufferView array, boolean last);
	public native boolean push(String string, boolean last);

	@JsProperty
	public native ArrayBufferView getResult();
}

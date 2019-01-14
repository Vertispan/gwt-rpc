/*
 * #%L
 * gwt-websockets-client
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
package com.colinalworth.gwt.websockets.client.impl;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(namespace = JsPackage.GLOBAL, isNative = true)
public class URL {
	public URL(String url, String base) {
	}
	public URL(String url) {
	}

	public String hash;
	public String host;
	public String hostname;
	public String href;
	public String origin;
	public String password;
	public String pathname;
	public String port;
	public String protocol;
	public String search;
//	public JsPropertyMap<String> searchParams;
	public String username;

	@JsMethod(name = "toString")
	public native String toString_();
}

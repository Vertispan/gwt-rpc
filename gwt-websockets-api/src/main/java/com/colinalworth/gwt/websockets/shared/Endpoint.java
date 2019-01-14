/*
 * #%L
 * gwt-websockets-api
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
package com.colinalworth.gwt.websockets.shared;

/**
 * Marks an interface as part of a pair to have serializers generated, and
 * easy wiring supplied to let this abstract out communication with its matching
 * endpoint.
 *
 * This and another matching interface should reference each other in the annotation
 * or in the generics of Client, Server (or future Worker, Page) interfaces. These
 * will may have no intrisic meaning, but make for an easier way to find a
 * matching implementation, or set up other generics in other tools.
 *
 * An endpoint object instance isn't usable by itself, but needs its counterpart
 * instance to function. For example, on the server, a Server instance would be
 * implemented and a Client type generated. A matched pair of instances would be
 * created and each assigned to the other when a new connect is created from a browser.
 *
 * On the client, the Server type is generated, while the Client is implemented.
 * To connect, the client code would ask for a Server connection to be made, and then
 * assign its own Client instance.
 */
public @interface Endpoint {
	/**
	 * Marker type for use in Endpoint#value to indicate that there is no other
	 * interface to process. Expected to be used for cases like RemoteService,
	 * where callbacks are the only way to communicate back with the client.
	 */
	interface NoRemoteEndpoint<T> {
		void setRemote(T remote);
		@RemoteEndpointSupplier
		T getRemote();
	}

	/**
	 * Describes the matching remote interface that will mirror this one. Optional,
	 * generally this will be implicitly recognized from the generics used when
	 * implementing Client or Server. Defaults to Object, meaning "unset".
	 */
	Class<?> value() default Object.class;

	/**
	 * Marks a method which will return the remote endpoint for this type. Intended for use
	 * when {@link Endpoint#value()} is assigned rather than extending an existing interface,
	 * so that the generated code knows where to check for the local copy of an endpoint.
	 *
	 * Can be omitted when value=NoRemoteEndpoint.
	 */
	public @interface RemoteEndpointSupplier {}

	@interface BaseClass {
		Class<?> value();
	}
}

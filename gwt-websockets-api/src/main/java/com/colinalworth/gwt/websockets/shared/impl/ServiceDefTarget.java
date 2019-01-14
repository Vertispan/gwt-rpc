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
package com.colinalworth.gwt.websockets.shared.impl;

public interface ServiceDefTarget {

	/**
	 * This exception is thrown when a service is invoked without
	 * {@link ServiceDefTarget#setServiceEntryPoint(String)} having been called.
	 */
	public static class NoServiceEntryPointSpecifiedException extends
			RuntimeException {

		public NoServiceEntryPointSpecifiedException() {
			super("Service implementation URL not specified");
		}
	}

	/**
	 * Return the strong name of the serialization policy to be used with this RPC
	 * instance.
	 */
	String getSerializationPolicyName();

	/**
	 * Gets the URL of a service implementation.
	 *
	 * @return the last value passed to {@link #setServiceEntryPoint(String)}
	 */
	String getServiceEntryPoint();

//	/**
//	 * Sets the RpcRequestBuilder that should be used by the service
//	 * implementation. This method can be called if customized request behavior is
//	 * desired. Calling this method with a null value will reset any custom
//	 * behavior to the default implementation.
//	 */
//	void setRpcRequestBuilder(RpcRequestBuilder builder);

	/**
	 * Sets the URL of a service implementation.
	 *
	 * @param address a URL that designates the service implementation to call
	 */
	void setServiceEntryPoint(String address);
}

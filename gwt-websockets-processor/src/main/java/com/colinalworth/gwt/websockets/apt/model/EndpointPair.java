/*
 * #%L
 * gwt-websockets-processor
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
package com.colinalworth.gwt.websockets.apt.model;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

public class EndpointPair {

	public static EndpointPair fromOne(Element annotatedEndpoint, ProcessingEnvironment processingEnvironment) {
		assert annotatedEndpoint.getKind() == ElementKind.INTERFACE;

		// see if this type extends another interface that describes how to implement it
		EndpointModel model = EndpointModel.from(annotatedEndpoint, processingEnvironment);

		// this will check for us that the other end exists, and matches correctly
		EndpointModel counterpart = model.getMatchingEndpoint(processingEnvironment);
		return new EndpointPair(model, counterpart);
	}

	private final EndpointModel left;
	private final EndpointModel right;

	private EndpointPair(EndpointModel endpoint1, EndpointModel endpoint2) {
		if (endpoint1.compareTo(endpoint2) > 0) {
			left = endpoint1;
			right = endpoint2;
		} else {
			left = endpoint2;
			right = endpoint1;
		}
	}

	public EndpointModel getLeft() {
		return left;
	}

	public EndpointModel getRight() {
		return right;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EndpointPair that = (EndpointPair) o;

		if (!left.equals(that.left)) return false;
		return right.equals(that.right);
	}

	@Override
	public int hashCode() {
		int result = left.hashCode();
		result = 31 * result + right.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "EndpointPair{" +
				"left=" + left +
				", right=" + right +
				'}';
	}
}

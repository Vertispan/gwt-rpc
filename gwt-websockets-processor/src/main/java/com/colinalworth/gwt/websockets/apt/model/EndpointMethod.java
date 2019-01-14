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

import com.colinalworth.gwt.websockets.shared.Callback;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EndpointMethod {

	private final ExecutableType mirror;
	private final ExecutableElement element;

	public EndpointMethod(ExecutableType method, ExecutableElement methodElt) {
		this.mirror = method;
		element = methodElt;
	}


	public void validate(ProcessingEnvironment env) {
	}

	public ExecutableType getMirror() {
		return mirror;
	}

	public ExecutableElement getElement() {
		return element;
	}

	public TypeName getCallbackTypeName(ProcessingEnvironment env) {
		assert hasCallback(env) : "No callback, can't return callback type";
		return ClassName.get(mirror.getParameterTypes().get(mirror.getParameterTypes().size() - 1));
	}

	public boolean hasCallback(ProcessingEnvironment env) {
		List<? extends TypeMirror> params = mirror.getParameterTypes();
		if (params.isEmpty()) {
			return false;
		}

		TypeMirror lastParamType = params.get(params.size() - 1);
		if (lastParamType.getKind() != TypeKind.DECLARED) {
			return false;
		}
		TypeMirror rawType = env.getTypeUtils().erasure(lastParamType);
		return ClassName.get(rawType).toString().equals(Callback.class.getName());
	}

	/**
	 * Returns a list of parameterized qualified types that will need to be read by
	 * code implementing this interface - the type params of the callbacks, if any.
	 *
	 * Note that Void is not omitted (but can only be serialized as null), but
	 * callbacks could skip writing (and reading) Voids.
	 */
	public List<TypeName> getTypesToRead(ProcessingEnvironment env) {
		if (hasCallback(env)) {
			TypeMirror callback = mirror.getParameterTypes().get(mirror.getParameterTypes().size() - 1);
			return ((DeclaredType) callback).getTypeArguments()
					.stream()
					.map(TypeName::get)
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	/**
	 * Returns a list of parameterized qualified types that will need to be written
	 * by code implementing this interface - the non-callback parameters of each
	 * method.
	 */
	public List<TypeName> getTypesToWrite(ProcessingEnvironment env) {
		int limit = hasCallback(env)
				? mirror.getParameterTypes().size() - 1
				: mirror.getParameterTypes().size();
		return mirror.getParameterTypes().stream()
				.limit(limit)
				.map(TypeName::get)
				.collect(Collectors.toList());
	}

	public TypeName getCallbackSuccessType(ProcessingEnvironment processingEnv) {
		assert hasCallback(processingEnv) : this;
		return getTypesToRead(processingEnv).get(0);
	}

	public TypeName getCallbackFailureType(ProcessingEnvironment processingEnv) {
		assert hasCallback(processingEnv) : this;
		return getTypesToRead(processingEnv).get(1);
	}

	public TypeName getReadingCallbackTypeName(ProcessingEnvironment processingEnv) {
		return ParameterizedTypeName.get(
				ClassName.get("com.colinalworth.gwt.websockets.shared.impl", "AbstractEndpointImpl", "ReadingCallback"),
				getCallbackSuccessType(processingEnv),
				getCallbackFailureType(processingEnv)
		);
	}

}

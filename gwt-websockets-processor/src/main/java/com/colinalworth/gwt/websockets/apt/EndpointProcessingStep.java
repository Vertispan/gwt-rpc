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
package com.colinalworth.gwt.websockets.apt;

import com.colinalworth.gwt.websockets.apt.model.EndpointMethod;
import com.colinalworth.gwt.websockets.apt.model.EndpointModel;
import com.colinalworth.gwt.websockets.apt.model.EndpointPair;
import com.colinalworth.gwt.websockets.shared.Endpoint;
import com.colinalworth.gwt.websockets.shared.Endpoint.NoRemoteEndpoint;
import com.colinalworth.gwt.websockets.shared.impl.AbstractEndpointImpl;
import com.colinalworth.gwt.websockets.shared.impl.AbstractNoRemoteImpl;
import com.colinalworth.gwt.websockets.shared.impl.AbstractRemoteServiceImpl;
import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.gwt.user.client.rpc.SerializationException;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.squareup.javapoet.TypeVariableName;
import com.vertispan.serial.SerializationStreamReader;
import com.vertispan.serial.SerializationStreamWriter;
import com.vertispan.serial.SerializationWiring;
import com.vertispan.serial.TypeSerializer;

import javax.annotation.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class EndpointProcessingStep implements ProcessingStep {
	private final ProcessingEnvironment processingEnv;

	public EndpointProcessingStep(ProcessingEnvironment processingEnv) {
		this.processingEnv = processingEnv;
	}

	@Override
	public Set<? extends Class<? extends Annotation>> annotations() {
		return Collections.singleton(Endpoint.class);
	}

	@Override
	public Set<? extends Element> process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {

		Set<Element> endpoints = elementsByAnnotation.get(Endpoint.class);

		endpoints.stream()
				.map(endpoint -> EndpointPair.fromOne(endpoint, processingEnv))
				.distinct()
				.forEach(pair -> {
					processingEnv.getMessager().printMessage(Kind.MANDATORY_WARNING, pair.toString());
					implement(pair.getLeft(), pair.getRight());
					implement(pair.getRight(), pair.getLeft());
				});

		return Sets.newHashSet();
	}

	private void implement(EndpointModel model, EndpointModel remoteModel) {
		// set up basics, declare superclass (and extra contract wiring?)
		String packageName = model.isPlaceholder() ? remoteModel.getPackage(processingEnv) : model.getPackage(processingEnv);
		String generatedTypeName = model.isPlaceholder() ? remoteModel.getGeneratedTypeName() + "Remote" : model.getGeneratedTypeName();
		Builder builder = TypeSpec.classBuilder(generatedTypeName)
				.addSuperinterface(model.isPlaceholder() ? ParameterizedTypeName.get(ClassName.get(NoRemoteEndpoint.class), remoteModel.getInterface()) : model.getInterface())
				.addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "\"$L\"", EndpointProcessor.class.getCanonicalName()).build())
				.addModifiers(Modifier.PUBLIC);

		// this is disgusting, need to find a way to make it part of the model, or
		// fork this type?
		if (model.isPlaceholder() || remoteModel.isPlaceholder()) {
			if (model.isPlaceholder()) {
				builder.superclass(ParameterizedTypeName.get(
						ClassName.get(AbstractNoRemoteImpl.class),
						remoteModel.getInterface()
				));
			} else if (remoteModel.isPlaceholder()) {
				builder.superclass(ParameterizedTypeName.get(
						//TODO read this from the endpointmodel somehow, hardcoding this is stupid
						ClassName.get(AbstractRemoteServiceImpl.class),
						ParameterizedTypeName.get(
								ClassName.get(NoRemoteEndpoint.class),
								model.getInterface()
						)
				));
			}
		} else {
			// finally the sane case, where we just ask the model and it works it out for us
			TypeName superclass = model.getSpecifiedSuperclass(processingEnv, remoteModel);
			if (superclass == null) {
				superclass = ClassName.get(AbstractEndpointImpl.class);
			}
			builder.superclass(superclass);
		}

		// create the serializer type
		TypeSpec serializer = declareSerializer(model, remoteModel, generatedTypeName + "Serializer");
		builder.addType(serializer);

		// create a field for the serializer
		ClassName serializerType = ClassName.get(packageName, generatedTypeName, serializer.name);
		builder.addField(
				serializerType,
				"s",
				Modifier.PRIVATE,
				Modifier.FINAL
		);

		// build constructors
		TypeVariableName sTypeVar = TypeVariableName.get("S", ClassName.get(SerializationStreamWriter.class));
		ParameterizedTypeName writerFactoryType = ParameterizedTypeName.get(
				ClassName.get(Function.class),
				ClassName.get(TypeSerializer.class),
				sTypeVar
		);
		ParameterizedTypeName sendType = ParameterizedTypeName.get(
				ClassName.get(Consumer.class),
				sTypeVar
		);
		ParameterizedTypeName onMessageType = ParameterizedTypeName.get(
				ClassName.get(BiConsumer.class),
				ParameterizedTypeName.get(
						Consumer.class,
						SerializationStreamReader.class
				),
				ClassName.get(TypeSerializer.class)
		);
		builder.addMethod(MethodSpec.constructorBuilder()
				.addTypeVariable(sTypeVar)
				.addParameter(writerFactoryType, "writerFactory")
				.addParameter(sendType, "send")
				.addParameter(onMessageType, "onMessage")
				.addStatement("this(writerFactory, send, new $T_Impl(), onMessage)", serializerType)
				.addModifiers(Modifier.PUBLIC)
				.build());
		builder.addMethod(MethodSpec.constructorBuilder()
				.addTypeVariable(sTypeVar)
				.addParameter(writerFactoryType, "writerFactory")
				.addParameter(sendType, "send")
				.addParameter(serializerType, "serializers")
				.addParameter(onMessageType, "onMessage")
				.addStatement("super(writerFactory, send, serializers.createSerializer(), onMessage)")
				.addStatement("s = serializers")
				.addModifiers(Modifier.PRIVATE)
				.build());

		// create each declared method
		List<EndpointMethod> endpointMethods = model.getEndpointMethods(processingEnv);
		for (int methodIndex = 0; methodIndex < endpointMethods.size(); methodIndex++) {
			EndpointMethod method = endpointMethods.get(methodIndex);
			MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getElement().getSimpleName().toString());
			List<? extends TypeMirror> parameterTypes = method.getMirror().getParameterTypes();
			for (int paramIndex = 0; paramIndex < parameterTypes.size(); paramIndex++) {
				TypeMirror param = parameterTypes.get(paramIndex);
				if (paramIndex == parameterTypes.size() - 1 && method.hasCallback(processingEnv)) {
					//last param is callback, name it so
					methodBuilder.addParameter(TypeName.get(param), "callback");

					continue;
				}
				methodBuilder.addParameter(TypeName.get(param), "arg" + paramIndex);
			}
			methodBuilder.addJavadoc(method.getElement().getEnclosingElement().getSimpleName().toString());

			methodBuilder.beginControlFlow("__send($L, () ->", methodIndex);

			// instead of using the actual params, using this so we don't attempt to write the callback
			List<? extends TypeName> parameterNames = method.getTypesToWrite(processingEnv);
			for (int paramIndex = 0; paramIndex < parameterNames.size(); paramIndex++) {
				TypeName paramTypeName = parameterNames.get(paramIndex);
				methodBuilder.addStatement("s.$L(arg$L, activeWriter)", writeMethodName(paramTypeName), paramIndex);
			}

			methodBuilder.endControlFlow();
			if (method.hasCallback(processingEnv)) {
				TypeSpec readingCallback = TypeSpec.anonymousClassBuilder("")
						.superclass(method.getReadingCallbackTypeName(processingEnv))
						.addMethod(MethodSpec.methodBuilder("success")
								.addParameter(SerializationStreamReader.class, "reader")
								.addModifiers(Modifier.PUBLIC)
								.addStatement("callback.onSuccess(s.$L(reader))", readMethodName(method.getCallbackSuccessType(processingEnv)))
								.build())
						.addMethod(MethodSpec.methodBuilder("failure")
								.addParameter(SerializationStreamReader.class, "reader")
								.addModifiers(Modifier.PUBLIC)
								.addStatement("callback.onFailure(s.$L(reader))", readMethodName(method.getCallbackFailureType(processingEnv)))
								.build())
						.build();
				methodBuilder.addCode(", $L", readingCallback);
			}
			methodBuilder.addStatement(")");

			builder.addMethod(methodBuilder
					.addModifiers(Modifier.PUBLIC)
					.build());
		}

		// build __invoke for our matching remote type
		MethodSpec.Builder invokeMethod = MethodSpec.methodBuilder("__invoke")
				.addParameter(int.class, "recipient")
				.addParameter(SerializationStreamReader.class, "reader")
				.addException(SerializationException.class)
				.addModifiers(Modifier.PROTECTED);
		CodeBlock.Builder invokeBody = CodeBlock.builder()
				.addStatement("int callbackId")
				.beginControlFlow("switch (recipient)");
		List<EndpointMethod> remoteEndpointMethods = remoteModel.getEndpointMethods(processingEnv);
		for (int i = 0; i < remoteEndpointMethods.size(); i++) {
			EndpointMethod remoteMethod = remoteEndpointMethods.get(i);
			invokeBody.beginControlFlow("case $L:", i);
			if (remoteMethod.hasCallback(processingEnv)) {
				invokeBody.add("// read callbackId first\n");
				invokeBody.addStatement("callbackId = reader.readInt()");
			}
			processingEnv.getMessager().printMessage(Kind.MANDATORY_WARNING, remoteMethod.getElement().getSimpleName());
			String remoteGetter = model.getRemoteEndpointGetterMethodName(processingEnv);
			invokeBody.add("$L().$L(", remoteGetter, remoteMethod.getElement().getSimpleName().toString());
			boolean first = true;
			// Note the use of "types to write" - this seems backward, but we're calling into the
			// interface, not generating it
			for (TypeName s : remoteMethod.getTypesToWrite(processingEnv)) {
				if (!first) {
					invokeBody.add(", ");
				}
				first = false;
				invokeBody.add("s.$L(reader)", readMethodName(s));
			}
			if (remoteMethod.hasCallback(processingEnv)) {
				if (!first) {
					invokeBody.add(", ");
				}
				TypeName callbackSuccessType = remoteMethod.getCallbackSuccessType(processingEnv);
				TypeName callbackFailureType = remoteMethod.getCallbackFailureType(processingEnv);
				invokeBody.add("$L",
						TypeSpec.anonymousClassBuilder("")
								.superclass(remoteMethod.getCallbackTypeName(processingEnv))
								.addMethod(MethodSpec.methodBuilder("onSuccess")
										.addParameter(callbackSuccessType, "value")
										.addModifiers(Modifier.PUBLIC)
										.addComment("indicate that a callback in in use (negative reference)")
										.beginControlFlow("__send(-callbackId, () ->")
										.addComment("indicate that the operation was a success")
										.addStatement("activeWriter.writeBoolean(true)")
										.addComment("write the value")
										.addStatement("s.$L(value, activeWriter)", writeMethodName(callbackSuccessType))
										.endControlFlow().addStatement(")")
										.build())
								.addMethod(MethodSpec.methodBuilder("onFailure")
										.addParameter(callbackFailureType, "error")
										.addModifiers(Modifier.PUBLIC)
										.addComment("indicate that a callback in in use (negative reference)")
										.beginControlFlow("__send(-callbackId, () ->")
										.addComment("indicate that the operation was a failure")
										.addStatement("activeWriter.writeBoolean(false)")
										.addComment("write the error")
										.addStatement("s.$L(error, activeWriter)", writeMethodName(callbackFailureType))
										.endControlFlow().addStatement(")")
										.build())
								.build());
			}

			invokeBody.addStatement(")");

			invokeBody.addStatement("break")
					.endControlFlow();
		}
		builder.addMethod(invokeMethod
				.addCode(invokeBody.endControlFlow().build())
				.build());

		// build __onError stub TODO what should be in this
		builder.addMethod(MethodSpec.methodBuilder("__onError")
				.addParameter(Throwable.class, "ex")
				.addModifiers(Modifier.PROTECTED)
				.build());

		// any extra contract methods?


		try {
			JavaFile.builder(packageName, builder.build()).build().writeTo(processingEnv.getFiler());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private TypeSpec declareSerializer(EndpointModel model, EndpointModel remoteModel, String serTypeName) {
		Builder builder = TypeSpec.interfaceBuilder(serTypeName)
				.addAnnotation(SerializationWiring.class)
				.addModifiers(Modifier.PUBLIC);

		builder.addMethod(MethodSpec.methodBuilder("createSerializer")
				.returns(TypeSerializer.class)
				.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
				.build());

		//create "write" methods
		Stream.of(
				model.getEndpointMethods(processingEnv).stream()
						.flatMap(m -> m.getTypesToWrite(processingEnv).stream()),
				remoteModel.getEndpointMethods(processingEnv).stream()
						.flatMap(m -> m.getTypesToRead(processingEnv).stream())
		)
				.flatMap(Function.identity())
				.distinct()
				.forEach(typeName -> {
					builder.addMethod(MethodSpec.methodBuilder(writeMethodName(typeName))
							.returns(void.class)
							.addParameter(typeName, "data")
							.addParameter(SerializationStreamWriter.class, "writer")
							.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
							.build());
				});

		//create "read" methods
		Stream.of(
				model.getEndpointMethods(processingEnv).stream()
						.flatMap(m -> m.getTypesToRead(processingEnv).stream()),
				remoteModel.getEndpointMethods(processingEnv).stream()
						.flatMap(m -> m.getTypesToWrite(processingEnv).stream())
		)
				.flatMap(Function.identity())
				.distinct()
				.forEach(typeName -> {
					builder.addMethod(MethodSpec.methodBuilder(readMethodName(typeName))
							.returns(typeName)
							.addParameter(SerializationStreamReader.class, "reader")
							.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
							.build());
				});

		return builder.build();
	}

	private String writeMethodName(TypeName typeName) {
		return "write" + typeName.toString().replaceAll("[^a-zA-Z0-9]", "_");
	}

	private String readMethodName(TypeName typeName) {
		return "read" + typeName.toString().replaceAll("[^a-zA-Z0-9]", "_");
	}
}

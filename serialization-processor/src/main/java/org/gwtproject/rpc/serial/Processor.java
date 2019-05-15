package org.gwtproject.rpc.serial;

import com.google.auto.service.AutoService;
import com.google.common.base.Charsets;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.Builder;
import org.dominokit.jacksonapt.DefaultJsonSerializationContext;
import org.gwtproject.rpc.gwtapt.JTypeUtils;
import org.gwtproject.rpc.serial.model.SerializableTypeModel;
import org.gwtproject.rpc.serial.model.SerializableTypeModel.Field;
import org.gwtproject.rpc.serial.model.SerializableTypeModel.Property;
import org.gwtproject.rpc.serial.processor.*;
import org.gwtproject.rpc.serialization.api.*;
import org.gwtproject.rpc.serialization.api.impl.TypeSerializerImpl;
import org.gwtproject.serial.json.Details;
import org.gwtproject.serial.json.Type;

import javax.annotation.Generated;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @todo this should probably be at least 2-4 other classes, not just one.
 */
@AutoService(javax.annotation.processing.Processor.class)
public class Processor extends AbstractProcessor {
    private static final String knownTypesFilename = "knownTypes.txt";

    private TypeElement serializationStreamReader;
    private TypeElement serializationStreamWriter;
    private TypeElement typeSerializer;
    private TypeElement fieldSerializer;
    private TypeElement serializationWiring;

    private Filer filer;
    private Messager messager;
    private Types types;
    private Elements elements;

    private Set<String> allTypes = new HashSet<>();


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        // Somewhat unusually, we request all types. It makes this processor more expensive than the usual one, but
        // we should be bound to only our project's sources.
        return Collections.singleton("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.singleton("serial.knownSubtypes");
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();

        try {
            String knownSubtypes = processingEnv.getOptions().get("serial.knownSubtypes");
            if (knownSubtypes != null) {
                allTypes.addAll(readTypes(Arrays.asList(knownSubtypes.split(File.pathSeparator))));
            }
        } catch (IOException e) {
            messager.printMessage(Kind.ERROR, "Failed to read from " + knownTypesFilename);
            e.printStackTrace();
        }

    }

    private void cacheHandyTypes() {
        serializationStreamReader = elements.getTypeElement(SerializationStreamReader.class.getName());
        serializationStreamWriter = elements.getTypeElement(SerializationStreamWriter.class.getName());
        typeSerializer = elements.getTypeElement(TypeSerializer.class.getName());
        fieldSerializer = elements.getTypeElement(FieldSerializer.class.getName());
        serializationWiring = elements.getTypeElement(SerializationWiring.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        cacheHandyTypes();
        // for each type we notice (not created by this processor?), note it in a file so we can work from it
        // later, during incremental updates to individual files (which could include creation of new files)

        // first though, we read the existing file, so that we can write updates to it.
        try {
            try {
                FileObject resource = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "", knownTypesFilename);
                Set<String> oldTypes = readTypes(resource);
                allTypes.addAll(oldTypes);
            } catch (IOException e) {
                //didn't exist yet, ignore
            }

            // then examine all classes we have been given
            Set<String> newTypes = collectTypes(roundEnv.getRootElements());
            allTypes.addAll(newTypes);

            if (roundEnv.processingOver()) {
                // write the new list of types to the file
                FileObject updated = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", knownTypesFilename);
                writeTypes(updated, allTypes);
                //TODO seems poor form to think no one might process based on us...
                return false;
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to update " + knownTypesFilename + ": " + e);
        }


        // continue processing with the full list of types if there was a change
        //TODO this is a little amateurish, try to keep the data collection more separate from the codegen...
        Map<TypeElement, Set<TypeElement>> subtypes = buildTypeTree(allTypes);
        for (Element element : roundEnv.getElementsAnnotatedWith(serializationWiring)) {

            SerializingTypes serializingTypes = new SerializingTypes(processingEnv.getTypeUtils(), processingEnv.getElementUtils(), subtypes);
            SerializableTypeOracleBuilder readStob = new SerializableTypeOracleBuilder(
                    processingEnv.getElementUtils(),
                    messager, serializingTypes
            );
            SerializableTypeOracleBuilder writeStob = new SerializableTypeOracleBuilder(
                    processingEnv.getElementUtils(),
                    messager, serializingTypes
            );

            // For each method that isn't createSerializer, it either reads or it writes. Methods of type
            // void are for writing, and take data as well as a writer, methods that return a type are for
            // reading, and only take a reader.
            // Pretty gross, but it will get us off the ground...

            for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
                if (method.getModifiers().contains(Modifier.STATIC) || method.getModifiers().contains(Modifier.DEFAULT)) {
                    continue;
                }
                if (isReadMethod(method)) {
                    readStob.addRootType(method.getReturnType());
                } else if (isWriteMethod(method)) {
                    for (VariableElement param : method.getParameters()) {
                        if (processingEnv.getTypeUtils().isSameType(param.asType(), serializationStreamWriter.asType())) {
                            continue;//that said, it should be the last param
                        }

                        writeStob.addRootType(param.asType());
                    }
                } else {
                    //confirm is createSerializer method - no params, returns a Serializer, otherwise error
                    if (!isSerializerFactoryMethod(method)) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Not a serializer factory method, write method, or read method", method);
                    }
                }
            }

            SerializableTypeOracle writeOracle;
            SerializableTypeOracle readOracle;
            try {
                writeOracle = writeStob.build();
                readOracle = readStob.build();
            } catch (UnableToCompleteException e) {
//                throw new RuntimeException(e);
                //already logged a message, just give up
                return false;
            }

            try {
                writeImpl(writeOracle, readOracle, element, serializingTypes);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }


        return false;
    }

    private void writeImpl(SerializableTypeOracle writeOracle, SerializableTypeOracle readOracle, Element serializationInterface, SerializingTypes serializingTypes) throws IOException {

        SerializableTypeOracleUnion bidiOracle = new SerializableTypeOracleUnion(readOracle, writeOracle);
        //write field serializers
        List<SerializableTypeModel> models = new ArrayList<>();
        for (TypeMirror serializableType : bidiOracle.getSerializableTypes()) {
            SerializableTypeModel model;
            if (serializableType.getKind() == TypeKind.ARRAY) {
                model = SerializableTypeModel.array(serializableType, bidiOracle, serializingTypes);
                writeArraySerializer(model);
            } else {
                assert serializableType.getKind() == TypeKind.DECLARED : serializableType.getKind();
                //get the element itself and write it
                model = SerializableTypeModel.create(
                        serializingTypes,
                        (TypeElement) ((DeclaredType) serializableType).asElement(),
                        messager,
                        bidiOracle.isSerializable(serializableType),
                        bidiOracle.maybeInstantiated(serializableType),
                        bidiOracle
                );
                writeFieldSerializer(model);
            }
            models.add(model);
        }


        String prefix = serializationInterface.getSimpleName().toString();
        String packageName = elements.getPackageOf(serializationInterface).getQualifiedName().toString();
        //write interface impl
        //TODO it would be lovely to have a metamodel...
        writeSerializerImpl(prefix, packageName, serializationInterface);

        //write type serializer, pointing at required field serializers and their appropriate use in each direction
        //TODO consider only doing this once, later, so we can be sure classes are still needed? not sure...
        writeTypeSerializer(prefix, packageName, models);

        // write out a JSON file describing which
        writeJsonManifest(prefix, packageName, models);
    }

    private void writeJsonManifest(String prefix, String packageName, List<SerializableTypeModel> models) throws IOException {
        Details d = new Details();
        d.setSerializerPackage(packageName);
        d.setSerializerInterface(prefix);

        Map<String, Type> serializableTypes = models.stream().map(stm -> {
            Type type = new Type();

            type.setName(stm.getTypeName().toString());

            if (stm.getCustomFieldSerializer() != null) {
                type.setCustomFieldSerializer(ClassName.get(stm.getCustomFieldSerializer()).toString());
            }

            type.setCanInstantiate(stm.mayBeInstantiated());
            type.setCanSerialize(stm.isSerializable());

            if (stm.getType().getKind() == TypeKind.ARRAY) {
                type.setKind(Type.Kind.ARRAY);
                type.setComponentTypeId(ClassName.get(((ArrayType) stm.getType()).getComponentType()).toString());
            } else if (stm.getTypeElement().getKind() == ElementKind.ENUM) {
                type.setKind(Type.Kind.ENUM);
                type.setEnumValues(stm.getTypeElement().getEnclosedElements().stream()
                        .filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
                        .map(e -> e.getSimpleName().toString())
                        .collect(Collectors.toList())
                );
            } else {
                type.setKind(Type.Kind.COMPOSITE);

                if (stm.getSuperclassFieldSerializer() != null) {
                    type.setSuperTypeId(ClassName.get(stm.getTypeElement().getSuperclass()).toString());
                }

                List<org.gwtproject.serial.json.Property> properties = new ArrayList<>();
                for (Field field : stm.getFields()) {
                    org.gwtproject.serial.json.Property p = new org.gwtproject.serial.json.Property();
                    p.setName(field.getName());
                    p.setTypeId(field.getTypeName().toString());
                    properties.add(p);
                }
                for (Property property : stm.getProperties()) {
                    org.gwtproject.serial.json.Property p = new org.gwtproject.serial.json.Property();
                    p.setName(property.getName());
                    p.setTypeId(property.getTypeName().toString());
                    properties.add(p);
                }
                type.setProperties(properties);
            }

            return type;
        }).collect(Collectors.toMap(Type::getName, Function.identity()));

        d.setSerializableTypes(serializableTypes);

        FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, packageName, prefix + ".json");
        try (PrintWriter writer = new PrintWriter(resource.openOutputStream())) {
            writer.print(Details.INSTANCE.write(d, DefaultJsonSerializationContext.builder()
                    .serializeNulls(false)
                    .indent(true)
                    .build()));
        }
    }

    private void writeSerializerImpl(String prefix, String packageName, Element serializationInterface) throws IOException {
        Builder implTypeBuilder = TypeSpec.classBuilder(prefix + "_Impl")
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "\"$L\"", Processor.class.getCanonicalName()).build())
                .addSuperinterface(ClassName.get(serializationInterface.asType()))
                .addModifiers(Modifier.PUBLIC);

        //exactly one method should return a TypeSerializer

        //the rest are either read or write methods
        //TODO again, metamodel?
        

        for (ExecutableElement method : ElementFilter.methodsIn(serializationInterface.getEnclosedElements())) {
            if (method.getModifiers().contains(Modifier.STATIC) || method.getModifiers().contains(Modifier.DEFAULT)) {
                continue;
            }
            if (types.isSameType(method.getReturnType(), typeSerializer.asType())) {
                implTypeBuilder.addMethod(MethodSpec.methodBuilder(method.getSimpleName().toString())
                        .returns(ClassName.get(typeSerializer))
                        .addModifiers(Modifier.PUBLIC)
                        .addStatement("return new $L_TypeSerializer()", prefix)
                        .build());
            } else {
                //read or write
                if (method.getReturnType().getKind() == TypeKind.VOID) {
                    TypeMirror writtenType = method.getParameters().get(0).asType();
                    //write(OneObject, SSW)
                    implTypeBuilder.addMethod(MethodSpec.methodBuilder(method.getSimpleName().toString())
                            .returns(TypeName.VOID)
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(ClassName.get(writtenType), "instance")
                            .addParameter(SerializationStreamWriter.class, "writer")
                            .beginControlFlow("try")
                            .addStatement("writer.write$L(instance)", SerializableTypeModel.getStreamMethodSuffix(writtenType, 0))
                            .nextControlFlow("catch ($T ex)", com.google.gwt.user.client.rpc.SerializationException.class)
                            .addStatement("throw new IllegalStateException(ex)")
                            .endControlFlow()
                            .build());
                } else {
                    TypeMirror readType = method.getReturnType();
                    //OneObject read(SSR)
                    implTypeBuilder.addMethod(MethodSpec.methodBuilder(method.getSimpleName().toString())
                            .returns(ClassName.get(readType))
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(SerializationStreamReader.class, "reader")
                            .beginControlFlow("try")
                            .addStatement("return ($T) reader.read$L()", ClassName.get(readType), SerializableTypeModel.getStreamMethodSuffix(readType, 0))
                            .nextControlFlow("catch ($T ex)", com.google.gwt.user.client.rpc.SerializationException.class)
                            .addStatement("throw new IllegalStateException(ex)")
                            .endControlFlow()
                            .build());
                }
            }

        }


        JavaFile.builder(packageName, implTypeBuilder.build()).build().writeTo(filer);
    }

    private void writeTypeSerializer(String prefix, String packageName, List<SerializableTypeModel> models) throws IOException {
        Builder typeSerializer = TypeSpec.classBuilder(prefix + "_TypeSerializer")
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "\"$L\"", Processor.class.getCanonicalName()).build())
                .superclass(TypeSerializerImpl.class)
                .addModifiers(Modifier.PUBLIC);

        //TODO this isn't optimal, but is easy to write quickly
        typeSerializer.addField(FieldSpec.builder(
                ParameterizedTypeName.get(Map.class, String.class, FieldSerializer.class),
                "fieldSerializer",
                Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC).initializer("new $T()", ClassName.get(HashMap.class)).build());

        CodeBlock.Builder clinit = CodeBlock.builder();
        for (SerializableTypeModel model : models) {
            if (model.mayBeInstantiated()) {
                clinit.addStatement("fieldSerializer.put($S, new $T())", model.getType(), model.getFieldSerializer());
            }
        }
        typeSerializer.addStaticBlock(clinit.build());

        typeSerializer.addMethod(MethodSpec.methodBuilder("serializer")
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(Override.class)
                .addParameter(String.class, "name")
                .returns(FieldSerializer.class)
                .addStatement("return fieldSerializer.computeIfAbsent(name, ignore -> {throw new IllegalArgumentException(name);})")
                .build());
        JavaFile.builder(packageName, typeSerializer.build()).build().writeTo(filer);
    }

    private void writeArraySerializer(SerializableTypeModel model) throws IOException {
        int rank = JTypeUtils.getRank(model.getType());
        assert rank > 0;
        TypeMirror componentType = JTypeUtils.getLeafType(model.getType());

        StringBuilder extraArrayRank = new StringBuilder();
        for (int i = 0; i < rank - 1; ++i) {
            extraArrayRank.append("[]");
        }

        // we get the specific field serializer we need, and then ask for its enclosing type - all of them get generated
        // whether or not we use them, in case another incantation of this processor ends up needing them
        ClassName fieldSerializerName = model.getFieldSerializer().enclosingClassName();
        String packageName = fieldSerializerName.packageName();

        TypeSpec.Builder fieldSerializerType = TypeSpec.classBuilder(fieldSerializerName)
                .addSuperinterface(ClassName.get(fieldSerializer))
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "\"$L\"", Processor.class.getCanonicalName()).build())
                .addModifiers(Modifier.PUBLIC);//TODO originating element if it is an element

        //write deserialize method
        MethodSpec.Builder deserializeMethodBuilder = MethodSpec.methodBuilder("deserialize")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(SerializationStreamReader.class, "reader")
                .addParameter(ClassName.get(model.getType()), "instance")
                .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                .addException(SerializationException.class);

        //TODO for readObject, share the Object_Array_CustomFieldSerializer
        deserializeMethodBuilder
                .beginControlFlow("for (int i = 0, n = instance.length; i < n; ++i)")
                .addStatement("instance[i] = ($T$L) reader.read$L()", componentType, extraArrayRank, SerializableTypeModel.getStreamMethodSuffix(componentType, rank - 1))
                .endControlFlow();

        fieldSerializerType.addMethod(deserializeMethodBuilder.build());


        //write serialize
        MethodSpec.Builder serializeMethodBuilder = MethodSpec.methodBuilder("serialize")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(SerializationStreamWriter.class, "writer")
                .addParameter(ClassName.get(model.getType()), "instance")
                .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                .addException(SerializationException.class);

        serializeMethodBuilder
                .addStatement("writer.writeInt(instance.length)")
                .beginControlFlow("for (int i = 0, n = instance.length; i < n; ++i)")
                .addStatement("writer.write$L(instance[i])", SerializableTypeModel.getStreamMethodSuffix(componentType, rank - 1))
                .endControlFlow();

        fieldSerializerType.addMethod(serializeMethodBuilder.build());

        //write instantiate

        MethodSpec.Builder instantiateMethodBuilder = MethodSpec.methodBuilder("instantiate")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(model.getType()))
                .addParameter(SerializationStreamReader.class, "reader")
                .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                .addException(SerializationException.class)
                .addStatement("int length = reader.readInt()")
                .addStatement("reader.claimItems(length)")
                .addStatement("return new $T[length]$L", ClassName.get(componentType), extraArrayRank);

        fieldSerializerType.addMethod(instantiateMethodBuilder.build());

        writeInstanceMethods(fieldSerializerType, model, true, true, true);

        JavaFile file = JavaFile.builder(packageName, fieldSerializerType.build()).build();

        try {
            file.writeTo(filer);
        } catch (FilerException ignore) {
            // someone already wrote this type - doesn't matter, should be consistent no matter who did it
        }
    }

    private void writeFieldSerializer(SerializableTypeModel model) throws IOException {
        //collect fields (err, properties for now)
        TypeSpec.Builder fieldSerializerType = TypeSpec.classBuilder(model.getFieldSerializerName())
                .addSuperinterface(ClassName.get(fieldSerializer))
                .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "\"$L\"", Processor.class.getCanonicalName()).build())
                .addModifiers(Modifier.PUBLIC)
                .addOriginatingElement(model.getTypeElement());
        boolean writeSerialize = false, writeDeserialize = false, writeInstantiate = false;
        if (model.getTypeElement().getKind() == ElementKind.ENUM) {
            //write deserialize method
            MethodSpec.Builder deserializeMethodBuilder = MethodSpec.methodBuilder("deserialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addParameter(SerializationStreamReader.class, "reader")
                    .addParameter(Object.class, "instance")
                    .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                    .addException(SerializationException.class);
            fieldSerializerType.addMethod(deserializeMethodBuilder.build());

            MethodSpec.Builder serializeMethodBuilder = MethodSpec.methodBuilder("serialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addParameter(SerializationStreamWriter.class, "writer")
                    .addParameter(Enum.class, "instance")
                    .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                    .addException(SerializationException.class).addStatement("writer.writeInt(instance.ordinal())");
            fieldSerializerType.addMethod(serializeMethodBuilder.build());
        } else {
            assert model.getTypeElement().getKind() == ElementKind.CLASS;

            writeSerialize = true;
            writeDeserialize = true;

            //write field accessors for violator stuff
            //TODO support private fields, not just the easy stuff. (then we can support final and other good fun)

            //write deserialize method
            MethodSpec.Builder deserializeMethodBuilder = MethodSpec.methodBuilder("deserialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addParameter(SerializationStreamReader.class, "reader")
//                    .addParameter(ClassName.get(typeElement), "instance")//this will be handled once we know what type we are dealing with below
                    .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                    .addException(SerializationException.class);

            if (model.getCustomFieldSerializer() != null) {
                TypeName paramType = model.getDeserializeMethodParamType().orElse(null);
                if (paramType == null) {
                    deserializeMethodBuilder.addParameter(Object.class, "unused");
                } else {
                    deserializeMethodBuilder.addParameter(paramType, "instance");
                    deserializeMethodBuilder.addStatement("$L.deserialize(reader, instance)", model.getCustomFieldSerializer());
                }
            } else {
                deserializeMethodBuilder.addParameter(model.getTypeName(), "instance");
                for (Property property : model.getProperties()) {
                    deserializeMethodBuilder.addStatement("instance.$L(($T) reader.read$L())", property.getSetter().getSimpleName(), property.getTypeName(), property.getStreamMethodName());
                }
                for (Field field : model.getFields()) {
                    deserializeMethodBuilder.addStatement("instance.$L = ($T) reader.read$L()", field.getField().getSimpleName(), field.getTypeName(), field.getStreamMethodName());
                }

                //walk up to superclass, if any
                if (model.getTypeElement().getSuperclass().getKind() != TypeKind.NONE && model.isSerializable()) {
                    deserializeMethodBuilder.addStatement("$L.deserialize(reader, instance)", model.getFieldSerializer().enclosingClassName());
                }
            }

            fieldSerializerType.addMethod(deserializeMethodBuilder.build());

            //write serialize
            MethodSpec.Builder serializeMethodBuilder = MethodSpec.methodBuilder("serialize")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addParameter(SerializationStreamWriter.class, "writer")
//                    .addParameter(ClassName.get(typeElement), "instance")//this will be handled once we know what type we are dealing with below
                    .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                    .addException(SerializationException.class);

            if (model.getCustomFieldSerializer() != null) {
                TypeName paramType = model.getSerializeMethodParamType().orElse(null);
                if (paramType == null) {
                    serializeMethodBuilder.addParameter(Object.class, "unused");
                } else {
                    serializeMethodBuilder.addParameter(paramType, "instance");
                    serializeMethodBuilder.addStatement("$L.serialize(writer, instance)", model.getCustomFieldSerializer());
                }
            } else {
                serializeMethodBuilder.addParameter(model.getTypeName(), "instance");
                for (Property property : model.getProperties()) {
                    serializeMethodBuilder.addStatement("writer.write$L(instance.$L())", property.getStreamMethodName(), property.getGetter().getSimpleName());
                }
                for (Field field : model.getFields()) {
                    serializeMethodBuilder.addStatement("writer.write$L(instance.$L)", field.getStreamMethodName(), field.getField().getSimpleName());
                }

                //walk up to superclass, if any
                if (model.getSuperclassFieldSerializer() != null) {
                    deserializeMethodBuilder.addStatement("$L.serialize(writer, instance)", model.getSuperclassFieldSerializer());
                }
            }
            
            fieldSerializerType.addMethod(serializeMethodBuilder.build());
        }
        //maybe write instantiate (if not abstract, and has default ctor) OR is an enum
        MethodSpec.Builder instantiateMethodBuilder = MethodSpec.methodBuilder("instantiate")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(Object.class)// could be ClassName.get(typeElement), if visible...
                .addParameter(SerializationStreamReader.class, "reader")
                .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                .addException(SerializationException.class);

        if (model.getCustomFieldSerializer() != null && CustomFieldSerializerValidator.hasInstantiationMethod(types, model.getCustomFieldSerializer(), model.getType())) {
            instantiateMethodBuilder.addStatement("return $L.instantiate(reader)", model.getCustomFieldSerializer());
        } else {
            if (model.getTypeElement().getKind() == ElementKind.ENUM || (!model.getTypeElement().getModifiers().contains(Modifier.ABSTRACT) && JTypeUtils.isDefaultInstantiable(model.getTypeElement()))) {
                writeInstantiate = true;
                //TODO support private and delegate to violator

                if (model.getTypeElement().getKind() == ElementKind.ENUM) {
                    instantiateMethodBuilder.addStatement("return $T.values()[reader.readInt()]", model.getTypeName());
                } else {
                    instantiateMethodBuilder.addStatement("return new $T()", model.getTypeName());
                }
            } else {
                //TODO actually handle writeInstantiate
                instantiateMethodBuilder.addStatement("throw new IllegalStateException(\"Not instantiable\")");
            }
        }
        fieldSerializerType.addMethod(instantiateMethodBuilder.build());

        writeInstanceMethods(fieldSerializerType, model, writeSerialize, writeDeserialize, writeInstantiate);

        String packageName = model.getFieldSerializerPackage();
        JavaFile file = JavaFile.builder(packageName, fieldSerializerType.build()).build();

        try {
            file.writeTo(filer);
        } catch (FilerException ignore) {
            // someone already wrote this type - doesn't matter, should be consistent no matter who did it
        }
    }


    /**
     * Write instance methods for field serializer, if required, in subclasses which support the operations needed.
     *
     * This exists so that the compiler can prune serialization code for objects that can only be deserialized, or
     * deserialization code for objects that can only be serialized. In classic RPC, this was managed through a
     * js/jsni map of method references, but that is not going to fly for J2CL, so instead the goal here is to
     * produce multiple field serializer types which each only contain the methods required for a particular use
     * case.
     *  @param fieldSerializerType the being built
     * @param dataType the type being de/serialized
     * @param writeSerialize whether or not it is necessary to serialize fields
     * @param writeDeserialize whether or not it is necessary to deserialize fields
     * @param writeInstantiate whether or not instantiation is supported
     */
    private void writeInstanceMethods(Builder fieldSerializerType, SerializableTypeModel dataType, boolean writeSerialize, boolean writeDeserialize, boolean writeInstantiate) {

        // cases that can be supported:
        //  * write object only
        //  * read object only, with instantiate
        //  * read object only, as superclass
        //  * write object, read object with instantiate
        //  * write object, read object as superclass
        //
        // this seems excessive, but it allows the compiler to remove unused methods based on the STOB,
        // rather than the native map approach of gwt2's rpc

//        writeInnerFieldSerializer(fieldSerializerType, dataType, true, true, false, false, "_WriteOnlyInstantiate");
        writeInnerFieldSerializer(fieldSerializerType, dataType, true, false, false, false, "WriteOnly");
        writeInnerFieldSerializer(fieldSerializerType, dataType, false, false, true, true, "ReadOnlyInstantiate");
        writeInnerFieldSerializer(fieldSerializerType, dataType, false, false, true, false, "ReadOnlySuperclass");
        writeInnerFieldSerializer(fieldSerializerType, dataType, true, true, true, true, "WriteInstantiateReadInstantiate");
//        writeInnerFieldSerializer(fieldSerializerType, dataType, true, false, true, true, "_WriteSuperclassReadInstantiate");
        writeInnerFieldSerializer(fieldSerializerType, dataType, true, true, true, false, "WriteInstantiateReadSuperclass");
//        writeInnerFieldSerializer(fieldSerializerType, dataType, true, false, true, false, "_WriteSuperclassReadSuperclass");


        // Now that we have those, the base class does nothing

    }

    private void writeInnerFieldSerializer(Builder fieldSerializerType, SerializableTypeModel dataType, boolean write, boolean ignore, boolean read, boolean instantiate, String nestedTypeName) {
        TypeSpec.Builder inner = TypeSpec.classBuilder(nestedTypeName).addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);
        inner.superclass(ClassName.get("", fieldSerializerType.build().name));
        if (write) {
            inner.addMethod(MethodSpec.methodBuilder("serial")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(SerializationStreamWriter.class, "writer")
                    .addParameter(Object.class, "instance")
                    .returns(TypeName.VOID)
                    .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                    .addException(SerializationException.class)
                    .addStatement("serialize(writer, ($T) instance)", dataType.getSerializeMethodParamType().orElse(ClassName.get(Object.class)))
                    .build());
        }
        if (read) {
            inner.addMethod(MethodSpec.methodBuilder("deserial")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(SerializationStreamReader.class, "reader")
                    .addParameter(Object.class, "instance")
                    .returns(TypeName.VOID)
                    .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                    .addException(SerializationException.class)
                    .addStatement("deserialize(reader, ($T)instance)", dataType.getDeserializeMethodParamType().orElse(ClassName.get(Object.class)))
                    .build());
        }
        if (instantiate) {
            inner.addMethod(MethodSpec.methodBuilder("create")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(SerializationStreamReader.class, "reader")
                    .returns(Object.class)
                    .addException(com.google.gwt.user.client.rpc.SerializationException.class)
                    .addException(SerializationException.class)
                    .addStatement("return instantiate(reader)")
                    .build());
        }

        fieldSerializerType.addType(inner.build());
    }

    // returns an object, to be read from the reader. Note that objects don't _have_ to be read this way, but it is an easy option
    private boolean isReadMethod(ExecutableElement method) {
        return method.getParameters().size() == 1 && types.isSameType(method.getParameters().get(0).asType(), serializationStreamReader.asType());
    }

    // returns void, takes several objects to write, with the final parameter being the writer
    private boolean isWriteMethod(ExecutableElement method) {
        return method.getReturnType().getKind() == TypeKind.VOID
                && method.getParameters().size() > 1
                && types.isSameType(method.getParameters().get(method.getParameters().size() - 1).asType(), serializationStreamWriter.asType());
    }

    // zero-arg method, returns the serializer that can be used
    private boolean isSerializerFactoryMethod(ExecutableElement method) {
        return method.getParameters().isEmpty() && types.isSameType(method.getReturnType(), typeSerializer.asType());
    }

    private Map<TypeElement, Set<TypeElement>> buildTypeTree(Set<String> allTypes) {
        // not sure we can do it sooner, so lets remove all unreachable types here, and build the tree
        Set<TypeElement> existingTypes = allTypes.stream()
                .map(typeName -> {
                    try {
                        TypeElement typeElement = elements.getTypeElement(typeName);
                        if (typeElement == null && typeName.contains("$")) {
                            typeElement = elements.getTypeElement(typeName.replaceAll("\\$", "."));
                        }
                        return typeElement;
                    } catch (Exception e) {
                        //ignore this type, we can't see or load the type, possibly something emulated?
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // for each type, if not already present, put it and all parents in the map
        HashMap<TypeElement, Set<TypeElement>> map = new HashMap<>();
        existingTypes.forEach(type -> appendWithParent(type, map));

        return map;
    }
    private void appendWithParent(TypeElement type, Map<TypeElement, Set<TypeElement>> map) {
        TypeMirror superclass = type.getSuperclass();
        if (superclass.getKind() == TypeKind.NONE) {
            //top of the tree, we're done
            return;
        }
        assert superclass.getKind() == TypeKind.DECLARED;
        TypeElement superclassElement = (TypeElement) ((DeclaredType) superclass).asElement();
        boolean added = map.computeIfAbsent(superclassElement, ignore -> new HashSet<>()).add(type);
        if (!added) {
            //we've already looked at this type, which means we've already added interfaces and parents too, give up early
            return;
        }
        appendWithParent(superclassElement, map);

        List<? extends TypeMirror> interfaces = type.getInterfaces();
        for (TypeMirror anInterface : interfaces) {
            TypeElement interfaceElement = (TypeElement) ((DeclaredType) anInterface).asElement();
            map.computeIfAbsent(interfaceElement, ignore -> new HashSet<>()).add(type);

            appendWithParent(interfaceElement, map);
        }


    }

    private void writeTypes(FileObject updated, Set<String> allTypes) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(updated.openOutputStream(), Charsets.UTF_8))) {
            writer.append("# Generated file, describing known types in the current project to\n");
            writer.append("# allow incremental code generation\n");

            for (String type : allTypes) {
                writer.write(type);
                writer.newLine();
            }
        }
    }

    private Set<String> collectTypes(Set<? extends Element> rootElements) {
        Set<TypeElement> types = new HashSet<>();
        new Scanner().scan(rootElements, types);
        return types.stream().map(elt -> elt.getQualifiedName().toString()).collect(Collectors.toSet());
    }

    private Set<String> readTypes(FileObject resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openInputStream(), Charsets.UTF_8))) {
            Set<String> lines = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                lines.add(line);
            }
            return lines;
        }
    }
    private Collection<? extends String> readTypes(List<String> knownSubtypePaths) throws IOException {
        Set<String> lines = new HashSet<>();
        for (String path : knownSubtypePaths) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), Charsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    lines.add(line);
                }
            }
        }
        return lines;
    }


    /**
     * Simple scanner to find only enums and classes - things the user might create or edit which we need to notice
     * from the bottom of the type hierarchy, up.
     */
    private static class Scanner extends ElementScanner8<Void, Set<TypeElement>> {
        @Override
        public Void visitType(TypeElement e, Set<TypeElement> typeElements) {
            if (e.getKind() == ElementKind.CLASS || e.getKind() == ElementKind.ENUM) {
                // mark classes and enums seen, since someone might avoid referencing either kind
                // directly, but have them extend/implement a serializable type
                typeElements.add(e);
            }
            return super.visitType(e, typeElements);
        }
    }
}

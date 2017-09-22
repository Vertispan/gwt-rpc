package com.vertispan.serial;

import com.google.auto.service.AutoService;
import com.google.common.base.Charsets;
import com.vertispan.serial.processor.SerializableTypeOracle;
import com.vertispan.serial.processor.SerializableTypeOracleBuilder;
import com.vertispan.serial.processor.UnableToCompleteException;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
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
import java.util.stream.Collectors;

@AutoService(javax.annotation.processing.Processor.class)
public class Processor extends AbstractProcessor {
    private static final String knownTypesFilename = "knownTypes.txt";

    private TypeElement serializationStreamReader;
    private TypeElement serializationStreamWriter;
    private TypeElement serializer;
    private TypeElement serializationWiring;

    private Filer filer;
    private Messager messager;
    private Types types;
    private Elements elements;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        // Somewhat unusually, we request all types. It makes this processor more expensive than the usual one, but
        // we should be bound to only our project's sources.
        return Collections.singleton("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();

        serializationStreamReader = elements.getTypeElement(SerializationStreamReader.class.getName());
        serializationStreamWriter = elements.getTypeElement(SerializationStreamWriter.class.getName());
        serializer = elements.getTypeElement(Serializer.class.getName());
        serializationWiring = elements.getTypeElement(SerializationWiring.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // for each type we notice (not created by this processor?), note it in a file so we can work from it
        // later, during incremental updates to individual files (which could include creation of new files)

        // first though, we read the existing file, so that we can write updates to it.
        Set<String> allTypes = new HashSet<>();
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
                //TODO  seems poor form to think no one might process based on us...
                return false;
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to update " + knownTypesFilename + ": " + e);
        }


        // continue processing with the full list of types if there was a change
        //TODO this is a little amateurish, try to keep the data collection more separate from the codegen...
        Map<TypeElement, Set<TypeElement>> subtypes = buildTypeTree(allTypes);
        for (Element element : roundEnv.getElementsAnnotatedWith(SerializationWiring.class)) {

            SerializableTypeOracleBuilder readStob = new SerializableTypeOracleBuilder(
                    processingEnv.getTypeUtils(),
                    processingEnv.getElementUtils(),
                    subtypes,
                    messager
            );
            SerializableTypeOracleBuilder writeStob = new SerializableTypeOracleBuilder(
                    processingEnv.getTypeUtils(),
                    processingEnv.getElementUtils(),
                    subtypes,
                    messager
            );

            // For each method that isn't createSerializer, it either reads or it writes. Methods of type
            // void are for writing, and take data as well as a writer, methods that return a type are for
            // reading, and only take a reader.
            // Pretty gross, but it will get us off the ground...

            for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
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


            writeImpl(writeOracle, readOracle, element);
        }


        return false;
    }

    private void writeImpl(SerializableTypeOracle writeOracle, SerializableTypeOracle readOracle, Element type) {
        
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
        return method.getParameters().isEmpty() && types.isSameType(method.getReturnType(), serializer.asType());
    }

    private Map<TypeElement, Set<TypeElement>> buildTypeTree(Set<String> allTypes) {
        // not sure we can do it sooner, so lets remove all unreachable types here, and build the tree
        Set<TypeElement> existingTypes = allTypes.stream()
                .map(typeName -> elements.getTypeElement(typeName))
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
            //TODO preamble, notice about what the file is for??

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
                lines.add(line);
            }
            return lines;
        }
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

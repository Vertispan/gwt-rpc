package com.vertispan.serial.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.vertispan.serial.processor.SerializableTypeOracleBuilder;
import com.vertispan.serial.processor.SerializingTypes;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SerializableTypeModel {
    public String getFieldSerializerName() {
        return type.getSimpleName() + "_FieldSerializer";
    }

    public static class Property {
        private final ExecutableElement setter;
        private final ExecutableElement getter;

        Property(ExecutableElement setter, ExecutableElement getter) {
            this.setter = setter;
            this.getter = getter;
        }

        public ExecutableElement getSetter() {
            return setter;
        }

        public ExecutableElement getGetter() {
            return getter;
        }

        public TypeName getTypeName() {
            return TypeName.get(getter.getReturnType());
        }

        //readFoo/writeFoo in stream reader and stream writer
        public String getStreamMethodName() {
            TypeKind kind = getter.getReturnType().getKind();
            if (kind.isPrimitive()) {
                return kind.name().substring(0, 1) + kind.name().substring(1).toLowerCase();
            } else if (ClassName.get(getter.getReturnType()).toString().equals("java.lang.String")) {
                return "String";
            } else {
                return "Object";
            }
        }
    }

    public static class Field {
        private final VariableElement field;

        Field(VariableElement field) {
            this.field = field;
        }

        public VariableElement getField() {
            return field;
        }

        public TypeName getTypeName() {
            return TypeName.get(field.asType());
        }

        //readFoo/writeFoo in stream reader and stream writer
        public String getStreamMethodName() {
            TypeKind kind = field.asType().getKind();
            if (kind.isPrimitive()) {
                return kind.name().substring(0, 1) + kind.name().substring(1).toLowerCase();
            } else if (ClassName.get(field.asType()).toString().equals("java.lang.String")) {
                return "String";
            } else {
                return "Object";
            }
        }
    }

    private TypeElement type;
    private TypeElement customFieldSerializer;
    private List<Property> properties;
    private List<Field> fields;//TODO do we need a real field? and how do we handle private?


    public static SerializableTypeModel create(SerializingTypes types, TypeElement serializableType) {
        TypeElement customFieldSerializer = SerializableTypeOracleBuilder.findCustomFieldSerializer(types, serializableType.asType());
        List<Property> properties = new ArrayList<>();
        List<Field> fields = new ArrayList<>();

        if (serializableType.getKind() == ElementKind.ENUM) {
            //nothing to serialize
            return new SerializableTypeModel(serializableType, customFieldSerializer, Collections.emptyList(), Collections.emptyList());
        }

        //rules of STOB are to look for fields,
        // * then invoke as properties if we can (at least on the jvm, but let's be consistent),
        // * else fall back to accessible fields,
        // * and then violator pattern - TODO

        for (VariableElement field : ElementFilter.fieldsIn(serializableType.getEnclosedElements())) {
            if (!SerializableTypeOracleBuilder.shouldConsiderForSerialization(field)) {
                 continue;
            }
            //TODO also consider @ConstructorProperties to write values, and read from getter or field
            //TODO how about builders?

            ExecutableElement setter = setter(field);
            ExecutableElement getter = getter(field);
            if (getter != null && setter != null) {
                properties.add(new Property(setter, getter));
                continue;
            }

            if (!field.getModifiers().contains(Modifier.PRIVATE)) {
                //use it as a field, just assign it directly
                fields.add(new Field(field));
            } else {
                //TODO violator pattern
                assert false : "field " + field + " is private";
            }
        }
        return new SerializableTypeModel(serializableType, customFieldSerializer, properties, fields);
    }

    private static ExecutableElement setter(VariableElement field) {
        return ElementFilter.methodsIn(field.getEnclosingElement().getEnclosedElements()).stream()
                .filter(m -> m.getParameters().size() == 1)
                .filter(m -> m.getSimpleName().contentEquals("set" + capitalize(field.getSimpleName().toString())))
                .findAny()
                .orElse(null);
    }
    private static ExecutableElement getter(VariableElement field) {
        return ElementFilter.methodsIn(field.getEnclosingElement().getEnclosedElements()).stream()
                .filter(m -> m.getParameters().isEmpty())
                .filter(m ->
                        m.getSimpleName().contentEquals("get" + capitalize(field.getSimpleName().toString()))
                        || m.getSimpleName().contentEquals("is" + capitalize(field.getSimpleName().toString()))
//                        || m.getSimpleName().contentEquals("has" + capitalize(field.getSimpleName().toString()))
                )
                .findAny()
                .orElse(null);
    }

    private static String capitalize(String s) {
        if (s.length() == 1) {
            return s.toUpperCase();
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }


    private SerializableTypeModel(TypeElement type, TypeElement customFieldSerializer, List<Property> properties, List<Field> fields) {
        this.type = type;
        this.customFieldSerializer = customFieldSerializer;
        this.properties = properties;
        this.fields = fields;
    }

    public TypeElement getType() {
        return type;
    }

    public TypeElement getCustomFieldSerializer() {
        return customFieldSerializer;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<Field> getFields() {
        return fields;
    }
}

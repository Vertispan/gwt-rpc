package org.gwtproject.rpc.serial.processor;

import com.squareup.javapoet.ClassName;
import org.gwtproject.rpc.gwtapt.JTypeUtils;
import org.gwtproject.rpc.serialization.api.SerializationStreamReader;
import org.gwtproject.rpc.serialization.api.SerializationStreamWriter;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Checks that a custom serializer is valid.
 */
public class CustomFieldSerializerValidator {
    private static final String NO_DESERIALIZE_METHOD =
            "Custom Field Serializer ''{0}'' does not define a deserialize method: ''public static void deserialize({1} reader,{2} instance)''";
    private static final String NO_INSTANTIATE_METHOD =
            "Custom Field Serializer ''{0}'' does not define an instantiate method: ''public static {1} instantiate({2} reader)''; but ''{1}'' is not default instantiable";
    private static final String NO_SERIALIZE_METHOD =
            "Custom Field Serializer ''{0}'' does not define a serialize method: ''public static void serialize({1} writer,{2} instance)''";
    private static final String TOO_MANY_METHODS =
            "Custom Field Serializer ''{0}'' defines too many methods named ''{1}''; please define only one method with that name";
    private static final String WRONG_CONCRETE_TYPE_RETURN =
            "Custom Field Serializer ''{0}'' returns the wrong type from ''concreteType''; return type must be ''java.lang.String''";

    public static ExecutableElement getConcreteTypeMethod(TypeElement serializer) {
        return ElementFilter.methodsIn(serializer.getEnclosedElements()).stream()
                .filter(m -> m.getSimpleName().toString().equals("concreteType"))
                .filter(m -> m.getParameters().isEmpty())
                .findAny().orElse(null);
    }

    public static ExecutableElement getDeserializationMethod(Types types, TypeElement serializer, TypeMirror serializee) {
        return getMethod(types, "deserialize", SerializationStreamReader.class.getName(), serializer,
                serializee)
                .orElseGet(() -> getMethod(types, "deserialize", com.google.gwt.user.client.rpc.SerializationStreamReader.class.getName(), serializer, serializee).orElse(null));
    }

    public static ExecutableElement getInstantiationMethod(Types types, TypeElement serializer, TypeMirror serializee) {
        ExecutableElement[] overloads = ElementFilter.methodsIn(serializer.getEnclosedElements()).stream().filter(m -> m.getSimpleName().toString().equals("instantiate")).toArray(ExecutableElement[]::new);
        for (ExecutableElement overload : overloads) {
            List<? extends VariableElement> parameters = overload.getParameters();

            if (parameters.size() != 1) {
                // Different overload
                continue;
            }

            String paramZero = ClassName.get(parameters.get(0).asType()).toString();
            if (!paramZero.equals(SerializationStreamReader.class.getName())
                    && !paramZero.equals(com.google.gwt.user.client.rpc.SerializationStreamReader.class.getName())) {
                // First param is not a stream class
                continue;
            }

            if (!isValidCustomFieldSerializerMethod(overload)) {
                continue;
            }

            TypeMirror type = overload.getReturnType();
            if (type.getKind().isPrimitive()) {
                // Primitives are auto serialized so this can't be the right method
                continue;
            }

            if (types.isAssignable(serializee, type)) {
                return overload;
            }
        }

        return null;
    }

    public static ExecutableElement getSerializationMethod(Types types, TypeElement serializer, TypeMirror serializee) {
        return getMethod(types, "serialize", SerializationStreamWriter.class.getName(), serializer, serializee)
                .orElseGet(() -> getMethod(types, "serialize", com.google.gwt.user.client.rpc.SerializationStreamWriter.class.getName(), serializer, serializee).orElse(null));
    }

    public static boolean hasDeserializationMethod(Types types, TypeElement serializer, TypeMirror serializee) {
        return getDeserializationMethod(types, serializer, serializee) != null;
    }

    public static boolean hasInstantiationMethod(Types types, TypeElement serializer, TypeMirror serializee) {
        return getInstantiationMethod(types, serializer, serializee) != null;
    }

    public static boolean hasSerializationMethod(Types types, TypeElement serializer, TypeMirror serializee) {
        return getSerializationMethod(types, serializer, serializee) != null;
    }

    /**
     * Returns a list of error messages associated with the custom field
     * serializer.
     *
     * @param serializer the class which performs the serialization
     * @param serializeeMirror the class being serialized
     * @return list of error messages, if any, associated with the custom field
     *         serializer
     */
    public static List<String> validate(Types types, TypeElement serializer, TypeMirror serializeeMirror) {
        List<String> reasons = new ArrayList<String>();

        Element serializee = types.asElement(serializeeMirror);
        if (serializee == null) {
            reasons.add(serializeeMirror.getKind() + " types cannot have custom field serializers");
            return reasons;
        }
        // If it isn't a declared type, we aren't interested, custom field serializers can't be used for
        // arrays, primitives, etc.


        if (serializee.getKind() == ElementKind.ENUM) {
            /*
             * Enumerated types cannot have custom field serializers because it would
             * introduce shared state between the client and the server via the
             * enumerated constants.
             */
            reasons.add("Enumerated types cannot have custom field serializers.");
            return reasons;
        }

        if (!hasDeserializationMethod(types, serializer, serializeeMirror)) {
            // No valid deserialize method was found.
            reasons.add(MessageFormat.format(NO_DESERIALIZE_METHOD, ClassName.get(serializer),
                    SerializationStreamReader.class.getName(), ClassName.get(serializeeMirror)));
        } else {
            checkTooMany("deserialize", serializer, reasons);
        }

        if (!hasSerializationMethod(types, serializer, serializeeMirror)) {
            // No valid serialize method was found.
            reasons.add(MessageFormat.format(NO_SERIALIZE_METHOD, ClassName.get(serializer),
                    SerializationStreamWriter.class.getName(), ClassName.get(serializeeMirror)));
        } else {
            checkTooMany("serialize", serializer, reasons);
        }

        if (!hasInstantiationMethod(types, serializer, serializeeMirror)) {
            boolean defaultInstantiable = JTypeUtils.isDefaultInstantiable(serializee);
            if (!defaultInstantiable && !serializee.getModifiers().contains(Modifier.ABSTRACT)) {
                // Not default instantiable and no instantiate method was found.
                reasons.add(MessageFormat.format(NO_INSTANTIATE_METHOD,
                        ClassName.get(serializer), ClassName.get(serializeeMirror),
                        SerializationStreamReader.class.getName()));
            }
        } else {
            checkTooMany("instantiate", serializer, reasons);
        }

        ExecutableElement concreteTypeMethod = getConcreteTypeMethod(serializer);
        if (concreteTypeMethod != null) {
            if (!"java.lang.String".equals(ClassName.get(concreteTypeMethod.getReturnType()).toString())) {
                // Wrong return type.
                reasons.add(MessageFormat.format(WRONG_CONCRETE_TYPE_RETURN, ClassName.get(serializer)));
            } else {
                checkTooMany("concreteType", serializer, reasons);
            }
        }

        return reasons;
    }

    private static void checkTooMany(String methodName, TypeElement serializer, List<String> reasons) {
        long count = ElementFilter.methodsIn(serializer.getEnclosedElements()).stream().filter(m -> m.getSimpleName().toString().equals(methodName)).count();
        if (count > 1) {
            reasons.add(MessageFormat.format(TOO_MANY_METHODS, ClassName.get(serializer.asType()),
                    methodName));
        }
    }

    private static Optional<ExecutableElement> getMethod(Types types, String methodName, String streamClassName,
                                                         TypeElement serializer, TypeMirror serializee) {
        ExecutableElement[] overloads = ElementFilter.methodsIn(serializer.getEnclosedElements()).stream().filter(m -> m.getSimpleName().toString().equals(methodName)).toArray(ExecutableElement[]::new);
        for (ExecutableElement overload : overloads) {
            List<? extends VariableElement> parameters = overload.getParameters();

            if (parameters.size() != 2) {
                // Different overload
                continue;
            }

            if (!ClassName.get(parameters.get(0).asType()).toString().equals(streamClassName)) {
                // First param is not a stream class
                continue;
            }

            VariableElement serializeeParam = parameters.get(1);
            TypeMirror type = serializeeParam.asType();
            if (type.getKind().isPrimitive()) {
                // Primitives are auto serialized so this can't be the right method
                continue;
            }

            // TODO: if isArray answered yes to isClass this cast would not be
            // necessary
            if (types.isAssignable(serializee, type)) {
                if (isValidCustomFieldSerializerMethod(overload)
                        && overload.getReturnType().getKind() == TypeKind.VOID) {
                    return Optional.of(overload);
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isValidCustomFieldSerializerMethod(ExecutableElement method) {
        if (method == null) {
            return false;
        }

        if (!method.getModifiers().contains(Modifier.STATIC)) {
            return false;
        }

        if (!method.getModifiers().contains(Modifier.PUBLIC)) {
            return false;
        }

        return true;
    }

    private CustomFieldSerializerValidator() {
    }
}


package com.vertispan.serial.processor;

import com.google.common.collect.Lists;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SerializingTypes {
    private final Types types;
    private final Elements elements;

    private final Map<TypeElement, List<TypeElement>> knownSubtypes;

    public SerializingTypes(Types types, Elements elements, Map<TypeElement, List<TypeElement>> knownSubtypes) {
        this.types = types;
        this.elements = elements;
        this.knownSubtypes = knownSubtypes;
    }

    public TypeElement getJavaLangObject() {
        return elements.getTypeElement(Object.class.getName());
    }

    public Types getTypes() {
        return types;
    }

    public Elements getElements() {
        return elements;
    }

    public List<TypeElement> getSubtypes(TypeMirror type) {
        TypeElement key = (TypeElement) types.asElement(type);
        return getSubtypes(key);
    }

    public List<TypeElement> getSubtypes(TypeElement type) {
        ArrayList<TypeElement> subtypes = Lists.newArrayList(knownSubtypes.getOrDefault(type, Collections.emptyList()));

        //TODO if unknown, we might need to walk through reflections of classpath stuff...

        //recursively add more
        int len = subtypes.size();
        for (int i = 0; i < len; i++) {
            subtypes.addAll(getSubtypes(subtypes.get(i)));
        }

        return subtypes;
    }
}

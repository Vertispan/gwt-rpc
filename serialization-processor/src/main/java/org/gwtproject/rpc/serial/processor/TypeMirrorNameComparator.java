package org.gwtproject.rpc.serial.processor;

import com.squareup.javapoet.ClassName;

import javax.lang.model.type.TypeMirror;
import java.util.Comparator;

/**
 * Compares {@link TypeMirror}s according to their qualified source names.
 */
public class TypeMirrorNameComparator implements Comparator<TypeMirror> {
    @Override
    public int compare(TypeMirror t1, TypeMirror t2) {
        return ClassName.get(t1).toString().compareTo(ClassName.get(t2).toString());
    }
}

package com.vertispan.gwtapt;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.ClassName;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.util.*;

/**
 * Some JClassType/JType bits that we take for granted in GWT Generators, expressed
 * in TypeMirror/Element.
 */
public class JTypeUtils {
    public static TypeMirror getLeafType(TypeMirror possibleArray) {
        TypeMirror next = possibleArray;
        while (next.getKind() == TypeKind.ARRAY) {
            next = ((ArrayType) next).getComponentType();
        }
        return next;
    }
    public static int getRank(TypeMirror possibleArray) {
        TypeMirror next = possibleArray;
        int rank = 0;
        while (next.getKind() == TypeKind.ARRAY) {
            next = ((ArrayType) next).getComponentType();
            rank++;
        }
        return rank;
    }

    /**
     *
     * @param types access to the type info
     * @param intfType in gwt, this is "this"
     * @param subType in gwt, this is the param passed in
     * @return
     */
    public static DeclaredType asParameterizationOf(Types types,
                                                    TypeMirror subType,
                                                    TypeMirror intfType) {
        for (TypeMirror supertype : getFlattenedSupertypeHierarchy(types, subType)) {
            if (supertype instanceof DeclaredType) {
                DeclaredType parameterized = (DeclaredType) supertype;
                //dodgy bit here, forcing them raw to compare their base types, is this safe?
                //answer: probably. We want to check if it is that particular interface, so we pick and return
                //        the same interface. That seems to match gwt's semantics.
                if (ClassName.get(types.erasure(intfType)).equals(ClassName.get(types.erasure(parameterized)))) {
                    // Found the desired supertype
                    return parameterized;
                }
                //TODO for the full gwt experience, allow for nested raw types, because who needs generics anyway
            }
        }
        return null;
    }

    public static List<? extends TypeMirror> findParameterizationOf(Types types,
                                                                    TypeMirror intfType,
                                                                    TypeMirror subType) {
        return new ArrayList<>(asParameterizationOf(types, intfType, subType).getTypeArguments());
    }

    public static DeclaredType asParameterizedByWildcards(Types types, DeclaredType type) {
        TypeMirror[] wildcards = type.getTypeArguments().stream().map(c -> {
            if (c.getKind() == TypeKind.TYPEVAR) {
                TypeMirror lb = ((TypeVariable) c).getLowerBound();
                TypeMirror ub = ((TypeVariable) c).getUpperBound();

                if (lb.getKind() == TypeKind.NULL) {
                    return types.getWildcardType(ub, null);
                }
                return types.getWildcardType(null, lb);
            }
            return types.getWildcardType(c, null);
        }).toArray(TypeMirror[]::new);
        return types.getDeclaredType((TypeElement) types.asElement(type), wildcards);
    }
    /**
     * Returns all of the superclasses and superinterfaces for a given type
     * including the type itself. The returned set maintains an internal
     * breadth-first ordering of the type, followed by its interfaces (and their
     * super-interfaces), then the supertype and its interfaces, and so on.
     */
    public static Collection<TypeMirror> getFlattenedSupertypeHierarchy(Types types, TypeMirror t) {
        List<TypeMirror> toAdd = new ArrayList<>();
        LinkedHashMap<String, TypeMirror> result = new LinkedHashMap<>();

        toAdd.add(t);

        for (int i = 0; i < toAdd.size(); i++) {
            TypeMirror type = toAdd.get(i);
            if (result.put(ClassName.get(types.erasure(type)).toString(), type) == null) {
                toAdd.addAll(types.directSupertypes(type));
            }
        }

        return result.values();
    }

    public static boolean isRawType(TypeMirror originalType) {
        if (originalType.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) originalType;
        return declaredType.getTypeArguments().size() != ((DeclaredType) declaredType.asElement().asType()).getTypeArguments().size();
    }

    /**
     * Determines if the class can be constructed using a simple <code>new</code>
     * operation. Specifically, the class must
     * <ul>
     * <li>be a class rather than an interface,</li>
     * <li>have either no constructors or a parameterless constructor, and</li>
     * <li>be a top-level class or a static nested class.</li>
     * </ul>
     *
     * @return <code>true</code> if the type is default instantiable, or
     *         <code>false</code> otherwise
     */
    public static boolean isDefaultInstantiable(DeclaredType type) {
        return isDefaultInstantiable(type.asElement());
    }
    /**
     * Determines if the class can be constructed using a simple <code>new</code>
     * operation. Specifically, the class must
     * <ul>
     * <li>be a class rather than an interface,</li>
     * <li>have either no constructors or a parameterless constructor, and</li>
     * <li>be a top-level class or a static nested class.</li>
     * </ul>
     *
     * @return <code>true</code> if the type is default instantiable, or
     *         <code>false</code> otherwise
     */
    public static boolean isDefaultInstantiable(Element type) {
        if (type.getKind() == ElementKind.INTERFACE) {
            return false;
        }
        if (!type.getModifiers().contains(Modifier.STATIC) && type.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            return false;
        }

        return ElementFilter.constructorsIn(type.getEnclosedElements())
                .stream()
                .anyMatch(ctor -> ctor.getParameters().isEmpty());
    }
}

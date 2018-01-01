package com.vertispan.serial.processor;

import com.squareup.javapoet.ClassName;
import com.vertispan.gwtapt.JTypeUtils;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractTypeVisitor8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * This class defines the method
 * {@link #constrainTypeBy(DeclaredType, DeclaredType)}.
 */
public class TypeConstrainer {
    /**
     * Check whether two base types have any subclasses in common. Note: this
     * could surely be implemented much more efficiently.
     */
    private static boolean baseTypesOverlap(SerializingTypes types, TypeMirror type1, TypeMirror type2) {
        assert (type1 == getBaseType(types.getTypes(), type1));
        assert (type2 == getBaseType(types.getTypes(), type2));

        if (ClassName.get(type1).toString().equals(ClassName.get(type2).toString())) {
            return true;
        }

        HashSet<TypeMirror> subtypes1 = new HashSet<>();
        subtypes1.add(type1);
        for (TypeElement sub1 : types.getSubtypes(type1)) {
            subtypes1.add(types.getTypes().erasure(sub1.asType()));
        }

        List<TypeMirror> subtypes2 = new ArrayList<>();
        subtypes2.add(type2);
        for (TypeElement sub2 : types.getSubtypes(type2)) {
            subtypes2.add(types.getTypes().erasure(sub2.asType()));
        }

        for (TypeMirror sub2 : subtypes2) {
            if (subtypes1.contains(sub2)) {
                return true;
            }
        }

        return false;
    }

    private static TypeMirror getBaseType(Types types, TypeMirror type) {
        return SerializableTypeOracleBuilder.getBaseType(types, type);
    }

    private static boolean isRealOrParameterized(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED;
//        if (type.isParameterized() != null) {
//            return true;
//        }
//        if (type instanceof JRealClassType) {
//            return true;
//        }
//        return false;
    }

//    /**
//     * Check whether <code>param</code> occurs anywhere within <code>type</code>.
//     */
//    private static boolean occurs(final JTypeParameter param, JClassType type) {
//        class OccursVisitor extends JTypeVisitor {
//            boolean foundIt = false;
//
//            @Override
//            public void endVisit(JTypeParameter seenParam) {
//                if (seenParam == param) {
//                    foundIt = true;
//                }
//            }
//        }
//
//        OccursVisitor visitor = new OccursVisitor();
//        visitor.accept(type);
//        return visitor.foundIt;
//    }

    private int freshTypeVariableCounter;

    private final SerializingTypes types;

    public TypeConstrainer(SerializingTypes serializingTypes) {
        this.types = serializingTypes;
    }

    /**
     * Return a subtype of <code>subType</code> that includes all values in both
     * <code>subType</code> and <code>superType</code>. The returned type must
     * have the same base type as <code>subType</code>. If there are definitely no
     * such values, return <code>null</code>.
     *
     * Impl detail from gwt-rpc - subType is at least partly wildcarded, specifically _at least_
     * the type params that are in superType, as we took the fully-wildcarded superType and found
     * all subtypes from that.
     */
    public TypeMirror constrainTypeBy(DeclaredType subType, DeclaredType superType) {
        if (superType.getTypeArguments().isEmpty()) {
            // If the supertype is not parameterized, it will not be possible to
            // constrain the subtype further.
            return subType;
        }

        // Replace each wildcard in the subType with a fresh type variable.
        // These type variables will be the ones that are constrained.
        Map<WildcardType, TypeMirror> constraints = new HashMap<>();
        DeclaredType subWithWildcardsReplaced =
                replaceWildcardsWithFreshTypeVariables(subType, constraints::put);

        // Rewrite subType so that it has the same base type as superType.
        DeclaredType subAsParameterized = JTypeUtils.asParameterizationOf(types.getTypes(), subWithWildcardsReplaced, superType);
//                subWithWildcardsReplaced.asParameterizationOf(super.getBaseType());
        if (subAsParameterized == null) {
            // The subtype's base does not inherit from the supertype's base,
            // so again no constraint will be possible.
            return subType;
        }

        // Check the rewritten type against superType
        if (!typesMatch(subAsParameterized, superType, constraints)) {
            // The types are completely incompatible
            return null;
        }

        // Apply the revised constraints to the original type
        return substitute(subWithWildcardsReplaced, toResolve -> {
            //TODO this is wrong, since we can't use typemirror types as keys like this...
            return Optional.ofNullable(constraints.get(toResolve));
        });
    }

    /**
     * Check whether two types can have any values in common. The
     * <code>constraints</code> field holds known constraints for type parameters
     * that appear in <code>type1</code>; this method may take advantage of those
     * constraints in its decision, and it may tighten them so long as the
     * tightening does not reject any values from the overlap of the two types.
     *
     * As an invariant, no key in <code>constraints</code> may occur inside any
     * value in <code>constraints</code>.
     *
     * Note that this algorithm looks for overlap matches in the arguments of
     * parameterized types rather than looking for exact matches. Looking for
     * overlaps simplifies the algorithm but returns true more often than it has
     * to.
     */
    boolean typesMatch(TypeMirror type1, TypeMirror type2, Map<WildcardType, TypeMirror> constraints) {
        if (types.getTypes().isSameType(type1, type2)) {
            // This covers the case where both are primitives, or otherwise recognized to be exactly the same already
            return true;
        }
        if (type1.getKind().isPrimitive() || type2.getKind().isPrimitive()) {
            // either only one is a primitive, or both are, but incompatible with each other
            return false;
        }

        //TODO finish this section
//        JTypeUtils.asParameterizedByWildcards(types.getTypes(), type1);
//        JTypeUtils.asParameterizedByWildcards(types.getTypes(), type2);
        //...
//        JGenericType type1Generic = type1.isGenericType();
//        if (type1Generic != null) {
//            return typesMatch(type1Generic.asParameterizedByWildcards(), type2, constraints);
//        }
//
//        JGenericType type2Generic = type2.isGenericType();
//        if (type2Generic != null) {
//            return typesMatch(type1, type2Generic.asParameterizedByWildcards(), constraints);
//        }

        if (type1.getKind() == TypeKind.WILDCARD) {
            WildcardType type1Wildcard = (WildcardType) type1;
            if (constraints.containsKey(type1)) {
//            JClassType type2Class = type2;
                TypeMirror type1Bound = constraints.get(type1Wildcard);
//            assert (!occurs(type1Parameter, type1Bound));
                if (!typesMatch(type1Bound, type2, constraints)) {
                    return false;
                }

                if (types.getTypes().isAssignable(type2, type1Bound)) {
                    constraints.put(type1Wildcard, type2);
                }
//            if (type1Bound.isAssignableFrom(type2Class)) {
//                constraints.put(type1Parameter, type2Class);
//            }
            }
            return typesMatch(type1Wildcard.getExtendsBound(), type2, constraints);
        }
//        JWildcardType type1Wild = type1.isWildcard();
//        if (type1Wild != null) {
//            return typesMatch(type1Wild.getUpperBound(), type2, constraints);
//        }

        if (type2.getKind() == TypeKind.WILDCARD) {
            return typesMatch(type1, ((WildcardType)type2).getExtendsBound(), constraints);
        }
//        JWildcardType type2Wild = type2.isWildcard();
//        if (type2Wild != null) {
//            return typesMatch(type1, type2Wild.getUpperBound(), constraints);
//        }

        //TODO detect and handle this?
//        JRawType type1Raw = type1.isRawType();
//        if (type1Raw != null) {
//            return typesMatch(type1Raw.asParameterizedByWildcards(), type2, constraints);
//        }
//
//        JRawType type2Raw = type2.isRawType();
//        if (type2Raw != null) {
//            return typesMatch(type1, type2Raw.asParameterizedByWildcards(), constraints);
//        }

        // The following assertions are known to be true, given the tests above.
        // assert (type1Generic == null);
        // assert (type2Generic == null);
        // assert (type1Wild == null);
        // assert (type2Wild == null);
        // assert (type1Raw == null);
        // assert (type2Raw == null);

        if (types.getTypes().isSameType(type1, types.getJavaLangObject().asType())) {
            return true;
        }

        if (types.getTypes().isSameType(type2, types.getJavaLangObject().asType())) {
            return true;
        }

//        TypeVariable type1Param = type1.isTypeParameter();
        if (type1.getKind() == TypeKind.TYPEVAR) {
            // It would be nice to check that type1Param's bound is a match
            // for type2, but that can introduce infinite recursions.
            return true;
        }

//        TypeVariable type2Param = type2.isTypeParameter();
        if (type2.getKind() == TypeKind.TYPEVAR) {
            // It would be nice to check that type2Param's bound is a match
            // for type1, but that can introduce infinite recursions.
            return true;
        }

        if (type1 instanceof ArrayType && type2 instanceof ArrayType) {
            if (typesMatch(((ArrayType) type1).getComponentType(), ((ArrayType) type2).getComponentType(), constraints)) {
                return true;
            }
        }

        if (isRealOrParameterized(type1) && isRealOrParameterized(type2)) {
            DeclaredType class1 = (DeclaredType) type1;
            DeclaredType class2 = (DeclaredType) type2;
            DeclaredType baseType1 = (DeclaredType) getBaseType(types.getTypes(), type1);
            DeclaredType baseType2 = (DeclaredType) getBaseType(types.getTypes(), type2);

            if (types.getTypes().isSameType(baseType1, baseType2)) {
                // type1 and type2 are parameterized types with the same base type;
                // compare their arguments
                List<? extends TypeMirror> args1 = class1.getTypeArguments();
                List<? extends TypeMirror> args2 = class2.getTypeArguments();
                boolean allMatch = true;
                for (int i = 0; i < args1.size(); i++) {
                    if (!typesMatch(args1.get(i), args2.get(i), constraints)) {
                        allMatch = false;
                    }
                }

                if (allMatch) {
                    return true;
                }
            } else {
                // The types have different base types, so just compare the base types
                // for overlap.
                if (baseTypesOverlap(types, baseType1, baseType2)) {
                    return true;
                }
            }
        }

        return false;
    }

//    /**
//     * The same as {@link #typesMatch(DeclaredType, DeclaredType, Map)}, but
//     * additionally support primitive types as well as class types.
//     */
//    boolean typesMatch(TypeMirror type1, TypeMirror type2, Map<TypeVariable, TypeMirror> constraints) {
//        if (types.getTypes().isSameType(type1, type2)) {
//            // This covers the case where both are primitives
//            return true;
//        }
//
//        if (type1 instanceof DeclaredType && type2 instanceof DeclaredType) {
//            return typesMatch((DeclaredType) type1, (DeclaredType) type2, constraints);
//        }
//
//        return false;
//    }

    /**
     * New docs: The old setup would apparently parameterize with wildcards, and then synthesize
     * new type params. In APT that doesn't seem possible, so we're taking the typemirror right
     * out of the typeelement and using the stock type params and their bounds.
     *
     * Formerly:
     * Replace all wildcards in <code>type</code> with a fresh type variable. For
     * each type variable created, add an entry in <code>constraints</code>
     * mapping the type variable to its upper bound.
     */
    private DeclaredType replaceWildcardsWithFreshTypeVariables(DeclaredType type,
                                                                final BiConsumer<WildcardType, TypeMirror> constraints) {

        type.accept(new AbstractTypeVisitor8<Void, Void>() {

            @Override
            public Void visitPrimitive(PrimitiveType t, Void aVoid) {
                return null;
            }

            @Override
            public Void visitNull(NullType t, Void aVoid) {
                return null;
            }

            @Override
            public Void visitArray(ArrayType t, Void aVoid) {
                t.accept(this, null);
                return null;
            }

            @Override
            public Void visitDeclared(DeclaredType t, Void aVoid) {
                t.getTypeArguments().forEach(arg -> arg.accept(this, null));
                return null;
            }

            @Override
            public Void visitError(ErrorType t, Void aVoid) {
                return null;
            }

            @Override
            public Void visitTypeVariable(TypeVariable t, Void aVoid) {
//                constraints.accept(t, t.getUpperBound());
                return null;
            }

            @Override
            public Void visitWildcard(WildcardType t, Void aVoid) {
                constraints.accept(t, t.getExtendsBound());
                return null;
            }

            @Override
            public Void visitExecutable(ExecutableType t, Void aVoid) {
                return null;
            }

            @Override
            public Void visitNoType(NoType t, Void aVoid) {
                return null;
            }

            @Override
            public Void visitUnion(UnionType t, Void aVoid) {
                return null;
            }

            @Override
            public Void visitIntersection(IntersectionType t, Void aVoid) {
                return null;
            }
        }, null);
        return type;

//        JModTypeVisitor replacer = new JModTypeVisitor() {
//            @Override
//            public void endVisit(JWildcardType wildcardType) {
//                // TODO: fix this to not assume the typemodel types.
//                com.google.gwt.dev.javac.typemodel.JTypeParameter newParam =
//                        new com.google.gwt.dev.javac.typemodel.JTypeParameter("TP$"
//                                + freshTypeVariableCounter++, -1);
//                newParam
//                        .setBounds(new com.google.gwt.dev.javac.typemodel.JClassType[] {(com.google.gwt.dev.javac.typemodel.JClassType) types
//                                .getJavaLangObject()});
//                constraints.put(newParam, wildcardType.getUpperBound());
//                replacement = newParam;
//            }
//        };
//
//        return replacer.transform(type);
    }

    /**
     * Substitute all occurrences in <code>type</code> of type parameters in
     * <code>constraints</code> for a wildcard bounded by the parameter's entry in
     * <code>constraints</code>. If the argument is <code>null</code>, return
     * <code>null</code>.
     */
    private TypeMirror substitute(DeclaredType type, final Function<WildcardType, Optional<TypeMirror>> resolver) {

        return type.accept(new TypeEditingVisitor(types.getTypes(), types.getElements()) {
            @Override
            public TypeMirror visitWildcard(WildcardType x, Void state) {
                return resolver.apply(x).map(t -> t.accept(this, null))
                        .orElseGet(() -> super.visitWildcard(x, state));
            }

            //            @Override
//            public TypeMirror visitTypeVariable(TypeVariable t, Void aVoid) {
//                //first, finish replacing it
//                TypeMirror cleaned = super.visitTypeVariable(t, aVoid);
//                if (cleaned instanceof TypeVariable) {
//                    return resolver.apply((TypeVariable)cleaned).orElse(t);
//                }
//                return cleaned;//?
//
////
////                return resolver.apply(t).map(t -> t.accept(this, null))
////                        .orElseGet(() -> super.visitTypeVariable(t, aVoid));
//            }
        }, null);

//        JModTypeVisitor substituter = new JModTypeVisitor() {
//            @Override
//            public void endVisit(JTypeParameter param) {
//                JClassType constr = constraints.get(param);
//                if (constr != null) {
//                    // further transform the substituted type recursively
//                    replacement = transform(constr);
//                }
//            }
//        };
//        return substituter.transform(type);
    }


    static class TypeEditingVisitor extends AbstractTypeVisitor8<TypeMirror, Void> {
        private final Types types;
        private final Elements elements;

        TypeEditingVisitor(Types types, Elements elements) {
            this.types = types;
            this.elements = elements;
        }

        @Override
        public TypeMirror visitIntersection(IntersectionType t, Void aVoid) {
            //TODO there doesn't seem to be a way to synthesize a new IntersectionType, so lets limit ourselves to the
            //     first bound - it is the only one allowed to be a class, so should be the most precise?
            return t.getBounds().get(0).accept(this, null);
        }

        @Override
        public TypeMirror visitPrimitive(PrimitiveType t, Void aVoid) {
            return t;
        }

        @Override
        public TypeMirror visitNull(NullType t, Void aVoid) {
            return t;
        }

        @Override
        public TypeMirror visitArray(ArrayType t, Void aVoid) {
            return types.getArrayType(t.getComponentType().accept(this, null));
        }

        @Override
        public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
            if (t.getTypeArguments().isEmpty()) {
                return t;
            }
            List<TypeMirror> newArgs = new ArrayList<TypeMirror>(t.getTypeArguments().size());
            for (TypeMirror original : t.getTypeArguments()) {
                // Are we looking at a self-parameterized type like Foo<T extends Foo<T>>?
                if (original.getKind().equals(TypeKind.TYPEVAR) && types.isAssignable(original, t)) {
                    // If so, return a raw type
                    return types.getDeclaredType((TypeElement) t.asElement());
                } else {
                    newArgs.add(original.accept(this, null));
                }
            }
            return types.getDeclaredType((TypeElement) t.asElement(), newArgs
                    .toArray(new TypeMirror[newArgs.size()]));
        }

        @Override
        public TypeMirror visitError(ErrorType t, Void aVoid) {
            return types.getNoType(TypeKind.NONE);//TODO?
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable t, Void aVoid) {
            if (t.equals(t.getUpperBound())) {
                // See comment in TransportableTypeVisitor (...in gwt rf apt), pasted here:
                /*
                 * Weird case seen in Eclipse with self-parameterized type variables such
                 * as <T extends Enum<T>>.
                 *
                 * TODO(bobv): Should intersection types be allowed at all? They don't
                 * seem to make much sense in the most-derived interface types, since the
                 * RF Generator won't know how to implement them.
                 */

                return types.erasure(t);
            }
            return t.getUpperBound().accept(this, null);
        }

        @Override
        public TypeMirror visitWildcard(WildcardType x, Void state) {
            // The JLS doesn't define erasure for wildcards [1], so don't rely on
            // javac's implementation. Instead, simplify wildcards as:
            // ? extends Foo -> erasure(Foo)
            // and: ? -> Object
            //
            // [1] https://bugs.openjdk.java.net/browse/JDK-8055054
            return x.getExtendsBound() != null
                    ? x.getExtendsBound().accept(this, state)
                    : types.getDeclaredType(elements.getTypeElement(Object.class.getCanonicalName()));
        }

        @Override
        public TypeMirror visitExecutable(ExecutableType t, Void aVoid) {
            return types.getNoType(TypeKind.NONE);//TODO? can't think of how this would matter
        }

        @Override
        public TypeMirror visitNoType(NoType t, Void aVoid) {
            return t;
        }

        @Override
        public TypeMirror visitUnion(UnionType t, Void aVoid) {
            return types.getNoType(TypeKind.NONE);//TODO? can't think of how this would matter
        }
    }
}

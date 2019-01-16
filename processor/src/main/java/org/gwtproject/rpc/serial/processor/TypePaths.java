package org.gwtproject.rpc.serial.processor;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;

/**
 * Paths of types and utility methods for creating them. These are used by
 * {@link SerializableTypeOracleBuilder} to record why it visits the types it
 * does.
 */
class TypePaths {
    /**
     * A path of types. This interface does not currently expose the type itself,
     * because these are currently only used for logging.
     */
    interface TypePath {
        /**
         * Get the previous element on this type path, or <code>null</code> if this
         * is a one-element path.
         */
        TypePath getParent();

        String toString();
    }

    static TypePaths.TypePath createArrayComponentPath(final ArrayType arrayType,
                                                       final TypePaths.TypePath parent) {
        assert (arrayType != null);

        return new TypePaths.TypePath() {
            public TypePaths.TypePath getParent() {
                return parent;
            }

            @Override
            public String toString() {
                return "Type '" + ClassName.get(arrayType.getComponentType())
                        + "' is reachable from array type '" + ClassName.get(arrayType)
                        + "'";
            }
        };
    }

    static TypePaths.TypePath createFieldPath(final TypePaths.TypePath parent, final VariableElement field) {
        return new TypePaths.TypePath() {
            public TypePaths.TypePath getParent() {
                return parent;
            }

            @Override
            public String toString() {
                TypeMirror type = field.asType();
                Element enclosingType = field.getEnclosingElement();
                return "'" + ClassName.get(type) + "' is reachable from field '"
                        + field.getSimpleName() + "' of type '" + ClassName.get(enclosingType.asType())
                        + "'";
            }
        };
    }

    static TypePaths.TypePath createRootPath(final TypeMirror type) {
        assert (type != null);

        return new TypePaths.TypePath() {
            public TypePaths.TypePath getParent() {
                return null;
            }

            @Override
            public String toString() {
                return "Started from '" + ClassName.get(type) + "'";
            }
        };
    }

    static TypePaths.TypePath createSubtypePath(final TypePaths.TypePath parent, final TypeMirror type,
                                                final TypeMirror supertype) {
        assert (type != null);
        assert (supertype != null);

        return new TypePaths.TypePath() {
            public TypePaths.TypePath getParent() {
                return parent;
            }

            @Override
            public String toString() {
                return "'" + ClassName.get(type)
                        + "' is reachable as a subtype of type '" + ClassName.get(supertype) + "'";
            }
        };
    }

    static TypePaths.TypePath createSupertypePath(final TypePaths.TypePath parent, final TypeMirror type,
                                                  final TypeMirror subtype) {
        assert (type != null);
        assert (subtype != null);

        return new TypePaths.TypePath() {
            public TypePaths.TypePath getParent() {
                return parent;
            }

            @Override
            public String toString() {
                return "'" + ClassName.get(type)
                        + "' is reachable as a supertype of type '" + ClassName.get(subtype) + "'";
            }
        };
    }

    static TypePaths.TypePath createTypeArgumentPath(final TypePaths.TypePath parent,
                                                     final DeclaredType baseType, final int typeArgIndex, final TypeMirror typeArg) {
        assert (baseType != null);
        assert (typeArg != null);

        return new TypePaths.TypePath() {
            public TypePaths.TypePath getParent() {
                return parent;
            }

            @Override
            public String toString() {
                return "'" + ClassName.get(typeArg)
                        + "' is reachable from type argument " + typeArgIndex + " of type '"
                        + ClassName.get(baseType) + "'";
            }
        };
    }

    static TypePaths.TypePath createTypeParameterInRootPath(final TypePaths.TypePath parent,
                                                            final TypeVariable typeParameter) {
        assert (typeParameter != null);

        return new TypePaths.TypePath() {
            public TypePaths.TypePath getParent() {
                return parent;
            }

            @Override
            public String toString() {
                //TODO
//                String parameterString = typeParameter.getName();
//                if (typeParameter.getDeclaringClass() != null) {
//                    parameterString +=
//                            " of class " + typeParameter.getDeclaringClass().getQualifiedSourceName();
//                }
                return "'" + typeParameter.getUpperBound()
                        + "' is reachable as an upper bound of type parameter " + typeParameter//should be parameterString
                        + ", which appears in a root type";
            }
        };
    }
}

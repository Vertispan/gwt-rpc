package org.gwtproject.rpc.serial.processor;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.*;

/**
 * Collection of utility methods for dealing with type hierarchies.
 */
class TypeHierarchyUtils {

    /**
     * Returns <code>true</code> if the type directly implements the specified
     * interface. The test is done on erased types; any paramaterizations supplied
     * in the arguments are ignored.
     *
     * @param type type to check
     * @param intf interface to look for
     * @return <code>true</code> if the type directly implements the specified
     *         interface
     */
    public static boolean directlyImplementsInterface(Types types, TypeElement type, TypeMirror intf) {
        Set<TypeMirror> seen = new HashSet<>();
        List<TypeMirror> interfaces = new ArrayList<>(type.getInterfaces());

        while (!interfaces.isEmpty()) {
            TypeMirror possibleMatch = types.erasure(interfaces.remove(0));
            if (!seen.add(possibleMatch)) {
                continue;
            }
            if (types.isSameType(possibleMatch, intf)) {
                return true;
            }
            interfaces.addAll(((TypeElement) types.asElement(possibleMatch)).getInterfaces());
        }

        return false;
    }

    /**
     * Returns all types on the path from the root type to the serializable
     * leaves.
     *
     * @param root the root type
     * @param leaves the set of serializable leaf types
     * @return all types on the path from the root type to the serializable leaves
     */
    public static List<TypeMirror> getAllTypesBetweenRootTypeAndLeaves(SerializingTypes serializingTypes, TypeMirror root,
                                                                       Collection<TypeMirror> leaves) {
        Map<TypeMirror, List<TypeMirror>> adjList = getInvertedTypeHierarchy(serializingTypes, serializingTypes.getTypes().erasure(root));
        Set<TypeMirror> types = new HashSet<>();

        for (TypeMirror type : leaves) {
            depthFirstSearch(types, adjList, serializingTypes.getTypes().erasure(type));
        }

        return Arrays.asList(types.toArray(new TypeMirror[0]));
    }

    /**
     * Returns the immediate subtypes of the erased class argument.
     */
    public static List<TypeMirror> getImmediateSubtypes(SerializingTypes types, TypeMirror clazz) {
        List<TypeMirror> immediateSubtypes = new ArrayList<>();
        clazz = types.getTypes().erasure(clazz);
        TypeElement clazzElement = (TypeElement) types.getTypes().asElement(clazz);
        for (TypeElement subclassElement : types.getSubtypes(clazz)) {//TODO verify that we are using this right w.r.t. erasure
            TypeMirror subclass = subclassElement.asType();
            TypeMirror superclass = subclassElement.getSuperclass();
            if (superclass != null && superclass.getKind() != TypeKind.NONE) {
                superclass = types.getTypes().erasure(superclass);
            }

            if (types.getTypes().isSameType(superclass,clazz) || clazzElement.getKind() == ElementKind.INTERFACE
                    && directlyImplementsInterface(types.getTypes(), (TypeElement) types.getTypes().asElement(subclass), clazz)) {
                immediateSubtypes.add(subclass);
            }
        }

        return immediateSubtypes;
    }

    private static void addEdge(Map<TypeMirror, List<TypeMirror>> adjList, TypeMirror subclass,
                                TypeMirror clazz) {
        List<TypeMirror> edges = adjList.get(subclass);
        if (edges == null) {
            edges = new ArrayList<>();
            adjList.put(subclass, edges);
        }

        edges.add(clazz);
    }

    private static void depthFirstSearch(Set<TypeMirror> seen,
                                         Map<TypeMirror, List<TypeMirror>> adjList, TypeMirror type) {
        if (seen.contains(type)) {
            return;
        }
        seen.add(type);

        List<TypeMirror> children = adjList.get(type);
        if (children != null) {
            for (TypeMirror child : children) {
                if (!seen.contains(child)) {
                    depthFirstSearch(seen, adjList, child);
                }
            }
        }
    }

//    private static boolean directlyImplementsInterfaceRecursive(Set<JClassType> seen,
//                                                                JClassType clazz, JClassType intf) {
//        assert (clazz.getErasedType() == clazz);
//        assert (intf.getErasedType() == intf);
//
//        if (clazz == intf) {
//            return true;
//        }
//
//        JClassType[] intfImpls = clazz.getImplementedInterfaces();
//
//        for (JClassType intfImpl : intfImpls) {
//            intfImpl = intfImpl.getErasedType();
//            if (!seen.contains(intfImpl)) {
//                seen.add(intfImpl);
//
//                if (directlyImplementsInterfaceRecursive(seen, intfImpl, intf)) {
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }

    /**
     * Given a root type return an adjacency list that is the inverted type
     * hierarchy.
     */
    private static Map<TypeMirror, List<TypeMirror>> getInvertedTypeHierarchy(SerializingTypes types, TypeMirror root) {
        Map<TypeMirror, List<TypeMirror>> adjList = new HashMap<>();
        Set<TypeMirror> seen = new HashSet<>();
        Stack<TypeMirror> queue = new Stack<>();
        queue.push(root);
        while (!queue.isEmpty()) {
            TypeMirror clazz = queue.pop();
            if (seen.contains(clazz)) {
                continue;
            }
            seen.add(clazz);

            List<TypeMirror> immediateSubtypes = getImmediateSubtypes(types, clazz);
            for (TypeMirror immediateSubtype : immediateSubtypes) {
                // Add an edge from the immediate subtype to the supertype
                addEdge(adjList, immediateSubtype, clazz);
                queue.push(immediateSubtype);
            }
        }

        return adjList;
    }

}


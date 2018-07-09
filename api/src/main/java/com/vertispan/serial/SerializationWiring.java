package com.vertispan.serial;

import java.lang.annotation.*;

/**
 * Indicates that the decorated interface should be used to generate a {@link TypeSerializer},
 * based on the methods and other annotations present.
 *
 * This is mostly an ugly strawman to start playing with ideas. Some constraints we're under:
 *  o  Annotations to whitelist doesn't seem like an option, they only allow raw types to be
 *     referenced.
 *  o  Unless a new final type was made for each "message", and then its fields considered instead
 *     of the type itself.
 *  o  Really don't want to enforce two copies of a given class that are the opposite of each
 *     other (ClientSerializer, ServerSerializer matching but opposite methods).
 *  o  Encourage/support "sharing" TypeSerializers between purposes ("read from server", "write
 *     to server" but also "read to IndexedDB" and "write to IndexedDB").
 *  o  Make it hard to accidentally leave out a type that would be obviously needed in the other
 *     direction.
 *
 * This is also meant to be a low-level tool, supporting other processors to generate this
 * annotation on a type, so they can decide what is serializable and easily declare it. They can
 * then paper over other details internally, like what format the data is in, etc.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface SerializationWiring {
//    Class<?>[] readBlacklist() default {};
//    Class<?>[] writeBlacklist() default {};
}

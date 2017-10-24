package com.vertispan.serial;

import java.lang.annotation.*;

/**
 * Indicates that the decorated interface should be used to generate a {@link TypeSerializer},
 * based on the methods and other annotations present.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface SerializationWiring {
    Class<?>[] readBlacklist() default {};
    Class<?>[] writeBlacklist() default {};
}

package com.vertispan.serial;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SerializationWiring {
    Class<?>[] readBlacklist() default {};
    Class<?>[] writeBlacklist() default {};
}

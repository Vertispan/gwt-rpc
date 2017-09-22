package com.vertispan.serial.processor;

import javax.lang.model.type.TypeMirror;

/**
 * Used to filter types out of serialization.
 */
interface TypeFilter {
    /**
     * Returns the name of this filter.
     *
     *
     * @return the name of this filter
     */
    String getName();

    /**
     * Returns <code>true</code> if the type should be included.
     *
     * @param type
     * @return <code>true</code> if the type should be included
     */
    boolean isAllowed(TypeMirror type);
}


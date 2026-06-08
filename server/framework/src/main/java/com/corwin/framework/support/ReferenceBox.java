package com.corwin.framework.support;

/**
 *
 * @author Corwin 2025/12/10
 */
public class ReferenceBox<T> {

    public T ref;

    private ReferenceBox(T ref) {
        this.ref = ref;
    }

    public static <T> ReferenceBox<T> of(T ref) {
        return new ReferenceBox<>(ref);
    }

}

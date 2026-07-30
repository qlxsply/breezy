package com.corwin.framework.support;

/**
 * A mutable reference box that allows reassigning a value inside a lambda or inner class.
 * <p>
 * Useful when you need to capture a result variable in a context that requires
 * effectively-final variables.
 *
 * @param <T> the referenced value type
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

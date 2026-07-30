package com.corwin.framework.scheduling;

import java.lang.annotation.*;

/**
 * Marker annotation for internal scheduled task methods.
 * <p>
 * When combined with {@link org.springframework.scheduling.annotation.Scheduled @Scheduled},
 * provides metadata (code, name, description) for monitoring and management.
 *
 * @author Corwin 2026/3/30
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InternalTask {

    /**
     * Unique task code identifier.
     */
    String code();

    /**
     * Human-readable task name.
     */
    String name();

    /**
     * Optional task description.
     */
    String description() default "";
}

package com.corwin.system.auth.published;

import com.corwin.framework.constant.UserType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that enforces authorization on a controller method or class,
 * requiring a specific user type and/or permission codes.
 *
 * @author Corwin 2026/4/19
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorize {

    /**
     * The required user type for access.
     */
    UserType userType() default UserType.ADMIN;

    /**
     * The permission codes required for access.
     */
    String[] permissions() default {};

    /**
     * Whether matching any single permission (instead of all) is sufficient.
     */
    boolean anyPermission() default false;
}

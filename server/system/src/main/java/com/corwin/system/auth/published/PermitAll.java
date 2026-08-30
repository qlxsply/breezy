package com.corwin.system.auth.published;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a controller method or class as publicly accessible without requiring
 * authentication or authorization.
 *
 * @author Corwin 2026/4/19
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PermitAll {}

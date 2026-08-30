package com.corwin.framework.web.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Corwin 2026/7/31
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveRequestBody {

  String ATTRIBUTE = SensitiveRequestBody.class.getName();
}

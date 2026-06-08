package com.corwin.system.audit.published;

import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Corwin 2026/4/19
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    AuditResource resource();

    AuditAction action();

    String description() default "";

    AuditLevel level() default AuditLevel.MEDIUM;

    boolean recordRequest() default true;

    boolean recordResponse() default false;
}

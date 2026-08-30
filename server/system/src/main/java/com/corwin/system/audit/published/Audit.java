package com.corwin.system.audit.published;

import com.corwin.system.audit.domain.model.AuditAction;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditResource;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a type or method for audit logging. Specifies the resource, action, severity
 * level, description, and whether to record request/response payloads in the audit trail.
 *
 * @author Corwin 2026/4/19
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

  /** The auditable resource type being accessed. */
  AuditResource resource();

  /** The auditable action performed on the resource. */
  AuditAction action();

  /**
   * Optional custom description of the audit event. If left empty, a default description is
   * generated from resource and action labels.
   */
  String description() default "";

  /** Severity level of this audit event. Defaults to {@link AuditLevel#MEDIUM}. */
  AuditLevel level() default AuditLevel.MEDIUM;

  /** Whether to record the request parameters and body in the audit log. */
  boolean recordRequest() default true;

  /** Whether to record the response body in the audit log. */
  boolean recordResponse() default false;
}

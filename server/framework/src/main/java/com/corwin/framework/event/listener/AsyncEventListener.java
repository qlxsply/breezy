package com.corwin.framework.event.listener;

import java.lang.annotation.*;

/**
 * Annotation marking a Spring bean method as an asynchronous event subscriber.
 *
 * <p>By default, each annotated method maps to its own consumer group.
 *
 * @author Corwin 2026/4/9
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncEventListener {

  /** Unique subscription ID. Generated from class + method + group info when empty. */
  String id() default "";

  /**
   * Raw consumer group identifier. Auto-derived from app name, class, method and event type when
   * empty.
   */
  String group() default "";

  /** Source whitelist; empty means no source filtering. */
  String[] sources() default {};

  /** Whether this listener is enabled. */
  boolean enabled() default true;

  /** Execution order within the same event type; lower values execute first. */
  int order() default 0;

  /**
   * Whether multiple listeners for the same event type may share a consumer group. Disabled by
   * default to prevent accidental consumption overlap.
   */
  boolean allowSharedGroup() default false;
}

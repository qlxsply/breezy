package com.corwin.system.resource.published;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to associate a REST controller with a specific API module.
 *
 * <p>Used at the type level to declare which {@link ApiModuleCode} a controller belongs to,
 * enabling module-based API grouping and filtering.
 *
 * @author Corwin 2026/4/16
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiMeta {
  /**
   * The module code that this annotated controller belongs to.
   *
   * @return the module code
   */
  ApiModuleCode module();
}

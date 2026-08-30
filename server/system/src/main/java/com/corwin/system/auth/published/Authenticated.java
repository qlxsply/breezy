package com.corwin.system.auth.published;

import com.corwin.framework.constant.UserType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that validates authentication on a controller method or class, optionally allowing
 * anonymous access or restricting authenticated users to a specific type.
 *
 * @author Corwin 2026/4/19
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authenticated {

  /** The required user type for access. */
  UserType userType() default UserType.GUEST;

  /** Whether the endpoint remains available while credentials must be updated. */
  boolean allowExpiredCredentials() default false;

  /**
   * Whether an anonymous request may proceed while authenticated requests still enforce the user
   * type.
   */
  boolean optional() default false;
}

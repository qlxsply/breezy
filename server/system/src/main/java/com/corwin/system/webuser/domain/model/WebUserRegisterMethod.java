package com.corwin.system.webuser.domain.model;

/**
 * Method used by the web user during registration (e.g. username+password, email, OAuth).
 *
 * @author Corwin 2026/5/11
 */
public enum WebUserRegisterMethod {
  USERNAME_PASSWORD,
  EMAIL_PASSWORD,
  PHONE_PASSWORD,
  OAUTH
}

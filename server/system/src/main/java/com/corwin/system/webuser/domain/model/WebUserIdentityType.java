package com.corwin.system.webuser.domain.model;

/**
 * Types of identity that can be used to identify a web user.
 * Supports USERNAME, EMAIL, PHONE, and OAUTH identity providers.
 *
 * @author Corwin 2026/5/11
 */
public enum WebUserIdentityType {
    USERNAME,
    EMAIL,
    PHONE,
    OAUTH
}

package com.corwin.system.webuser.domain.model;

/**
 * Types of lifecycle events that can be recorded for a web user account.
 *
 * @author Corwin 2026/5/11
 */
public enum WebUserLifecycleEventType {
    REGISTERED,
    LOGIN_SUCCESS,
    PASSWORD_CHANGED,
    PROFILE_UPDATED,
    LOGOUT,
    DISABLED,
    ENABLED,
    CANCELLED
}

package com.corwin.system.webuser.domain.model;

/**
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

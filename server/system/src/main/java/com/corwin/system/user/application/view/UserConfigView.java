package com.corwin.system.user.application.view;

/**
 * View object representing a user configuration entry with code, description, value type, and
 * value.
 *
 * @author Corwin 2026/3/30
 */
public record UserConfigView(String code, String description, String valueType, String value) {}

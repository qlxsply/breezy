package com.corwin.bootstrap.application;

/**
 * @author Corwin 2026/5/5
 */
public enum BootstrapTaskKey {
    SCHEMA_SYNC("schema"),
    CONFIG_SYNC("config"),
    API_SYNC("api"),
    RESOURCE_SYNC("resource"),
    DICTIONARY_SYNC("dict"),
    DEFAULT_USER_SYNC("users"),
    SYSTEM_FILE_SYNC("files");

    private final String cliValue;

    BootstrapTaskKey(String cliValue) {
        this.cliValue = cliValue;
    }

    public String cliValue() {
        return cliValue;
    }

    public static BootstrapTaskKey fromCliValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        for (BootstrapTaskKey item : values()) {
            if (item.cliValue.equals(normalized)) {
                return item;
            }
        }
        return null;
    }
}

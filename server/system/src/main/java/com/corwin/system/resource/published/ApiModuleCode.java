package com.corwin.system.resource.published;

/**
 * @author Corwin 2026/4/16
 */
public enum ApiModuleCode {
    DEFAULT("default"),
    SYSTEM("system"),
    DATASOURCE("datasource"),
    SCHEMAFORGE("schemaforge"),
    REMINDER("reminder"),
    STORAGE("storage"),
    JSONFMT("jsonfmt"),
    METHODSTAT("methodstat"),
    WEB("web");

    private final String code;

    ApiModuleCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

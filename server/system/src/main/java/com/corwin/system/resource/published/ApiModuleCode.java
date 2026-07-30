package com.corwin.system.resource.published;

/**
 * API module code constants.
 *
 * <p>Defines the set of recognized module codes used to classify API endpoints
 * into functional domains such as system, datasource, storage, etc.</p>
 *
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

    /**
     * Returns the module code string.
     *
     * @return the module code
     */
    public String code() {
        return code;
    }
}

package com.corwin.system.file.published;

/**
 * @author Corwin 2026/4/16
 */
public enum InternalFileType {
    TEXT("text/plain; charset=UTF-8", ".txt", "Text"),
    JSON("application/json", ".json", "JSON"),
    CSV("text/csv; charset=UTF-8", ".csv", "CSV"),
    EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx", "Excel"),
    WORD_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx", "Word"),
    PDF("application/pdf", ".pdf", "PDF"),
    YAML("application/yaml", ".yaml", "YAML"),
    XML("application/xml", ".xml", "XML"),
    SQL("application/sql", ".sql", "SQL");

    private final String contentType;
    private final String defaultExtension;
    private final String desc;

    InternalFileType(String contentType, String defaultExtension, String desc) {
        this.contentType = contentType;
        this.defaultExtension = defaultExtension;
        this.desc = desc;
    }

    public String contentType() {
        return contentType;
    }

    public String defaultExtension() {
        return defaultExtension;
    }

    public String desc() {
        return desc;
    }
}

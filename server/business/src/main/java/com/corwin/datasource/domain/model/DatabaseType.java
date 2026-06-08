package com.corwin.datasource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 *
 * @author Corwin 2026/1/11
 */
public enum DatabaseType implements DictEnumDefinition {
    H2("H2", DictTagColor.SLATE, DictTagType.INFO),
    MYSQL("MySQL", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
    POSTGRESQL("PostgreSQL", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
    ORACLE("Oracle", DictTagColor.DANGER_RED, DictTagType.DANGER),
    SQLSERVER("SQL Server", DictTagColor.PURPLE, DictTagType.INFO),
    ;

    private final String label;
    private final DictTagColor tagColor;
    private final DictTagType tagType;

    DatabaseType(String label, DictTagColor tagColor, DictTagType tagType) {
        this.label = label;
        this.tagColor = tagColor;
        this.tagType = tagType;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String tagColor() {
        return tagColor.itemValue();
    }

    @Override
    public String tagType() {
        return tagType.itemValue();
    }

    public static DatabaseType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        String lower = jdbcUrl.toLowerCase();
        if (lower.startsWith("jdbc:h2:")) {
            return H2;
        }
        if (lower.startsWith("jdbc:mysql:")) {
            return MYSQL;
        }
        if (lower.startsWith("jdbc:postgresql:")) {
            return POSTGRESQL;
        }
        if (lower.startsWith("jdbc:oracle:")) {
            return ORACLE;
        }
        if (lower.startsWith("jdbc:sqlserver:") || lower.startsWith("jdbc:microsoft:sqlserver:")) {
            return SQLSERVER;
        }
        return null;
    }
}

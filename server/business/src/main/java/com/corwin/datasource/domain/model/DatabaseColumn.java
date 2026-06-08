package com.corwin.datasource.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * 数据库列
 *
 * @author Corwin 2026/1/11
 */
@Getter
@Entity
@Table(name = "db_column", indexes = {@Index(name = "idx_db_column_db", columnList = "database_id"),
        @Index(name = "idx_db_column_table", columnList = "table_id"),
        @Index(name = "idx_db_column_name", columnList = "column_name")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_db_column", columnNames = {"table_id", "column_name"})})
public class DatabaseColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属数据库ID
     */
    @Column(name = "database_id", nullable = false)
    private Long databaseId;

    /**
     * 数据源ID
     */
    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "column_name", nullable = false, length = 256)
    private String columnName;

    /**
     * 别名
     */
    @Column(name = "alias", length = 128)
    private String alias;

    /**
     * JDBC getColumns 返回的 TYPE_NAME
     */
    @Column(name = "type_name", length = 128)
    private String typeName;

    /**
     * java.sql.Types 对应数值
     */
    @Column(name = "jdbc_type")
    private Integer jdbcType;

    @Column(name = "column_size")
    private Integer columnSize;

    @Column(name = "decimal_digits")
    private Integer decimalDigits;

    @Column(name = "nullable_flag")
    private Boolean nullable;

    @Column(name = "ordinal_position")
    private Integer ordinalPosition;

    @Column(name = "default_value", length = 1000)
    private String defaultValue;

    /**
     * 数据库原生备注
     */
    @Column(name = "remark_db", length = 1000)
    private String remarkDb;

    /**
     * 自定义备注
     */
    @Column(name = "remark_custom", length = 1000)
    private String remarkCustom;

    protected DatabaseColumn() {
    }

    public static DatabaseColumn create(Long dataSourceId, Long databaseId, Long tableId, String columnName,
            String typeName, Integer jdbcType, Integer columnSize, Integer decimalDigits, Boolean nullable,
            Integer ordinalPosition, String defaultValue, String remarkDb) {
        DatabaseColumn c = new DatabaseColumn();
        c.dataSourceId = dataSourceId;
        c.databaseId = databaseId;
        c.tableId = tableId;
        c.columnName = columnName;
        c.typeName = typeName;
        c.jdbcType = jdbcType;
        c.columnSize = columnSize;
        c.decimalDigits = decimalDigits;
        c.nullable = nullable;
        c.ordinalPosition = ordinalPosition;
        c.defaultValue = defaultValue;
        c.remarkDb = remarkDb;
        return c;
    }

    public void updateBasicInfo(String alias, String remarkCustom) {
        this.alias = alias;
        this.remarkCustom = remarkCustom;
    }

    public void updateRemarkCustom(String remarkCustom) {
        this.remarkCustom = remarkCustom;
    }
}

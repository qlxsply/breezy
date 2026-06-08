package com.corwin.datasource.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * 数据库表
 *
 * @author Corwin 2026/1/11
 */
@Getter
@Entity
@Table(name = "db_table", indexes = {@Index(name = "idx_db_table_db", columnList = "database_id"),
        @Index(name = "idx_db_table_name", columnList = "table_name")}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_db_table",
                columnNames = {"database_id", "table_catalog", "table_schema", "table_name"})})
public class DatabaseTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属数据库ID
     */
    @Column(name = "database_id", nullable = false)
    private Long databaseId;

    /**
     * 数据源ID（冗余字段，便于清理或跨库查询）
     */
    @Column(name = "data_source_id", nullable = false)
    private Long dataSourceId;

    /**
     * 别名
     */
    @Column(name = "alias", length = 128)
    private String alias;

    /**
     * JDBC 元数据字段
     */
    @Column(name = "table_catalog", length = 128)
    private String tableCatalog;

    @Column(name = "table_schema", length = 128)
    private String tableSchema;

    @Column(name = "table_name", nullable = false, length = 256)
    private String tableName;

    /**
     * TABLE / VIEW 等
     */
    @Column(name = "table_type", length = 32)
    private String tableType;

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

    protected DatabaseTable() {
    }

    public static DatabaseTable create(Long dataSourceId, Long databaseId, String tableCatalog, String tableSchema,
            String tableName, String tableType, String remarkDb) {
        DatabaseTable t = new DatabaseTable();
        t.dataSourceId = dataSourceId;
        t.databaseId = databaseId;
        t.tableCatalog = tableCatalog;
        t.tableSchema = tableSchema;
        t.tableName = tableName;
        t.tableType = tableType;
        t.remarkDb = remarkDb;
        return t;
    }

    public void updateBasicInfo(String alias, String remarkCustom) {
        this.alias = alias;
        this.remarkCustom = remarkCustom;
    }

    public void updateRemarkCustom(String remarkCustom) {
        this.remarkCustom = remarkCustom;
    }
}

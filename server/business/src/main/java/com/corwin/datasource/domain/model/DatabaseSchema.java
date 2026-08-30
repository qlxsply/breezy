package com.corwin.datasource.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * 被管理的数据源中的数据库/模式
 *
 * @author Corwin 2026/1/11
 */
@Getter
@Entity
@Table(
    name = "db_schema",
    indexes = {@Index(name = "idx_db_schema_source", columnList = "data_source_id")},
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_db_schema_name",
          columnNames = {"data_source_id", "database_name"})
    })
public class DatabaseSchema {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 数据源ID */
  @Column(name = "data_source_id")
  private Long dataSourceId;

  /** 物理数据库/模式名 */
  @Column(name = "database_name", nullable = false, length = 128)
  private String databaseName;

  /** 别名 */
  @Column(name = "alias", length = 128)
  private String alias;

  /** 自定义备注 */
  @Column(name = "remark_custom", length = 512)
  private String remarkCustom;

  /** 数据库产品名 */
  @Column(name = "product_name", length = 256)
  private String productName;

  /** 数据库产品版本 */
  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "product_version")
  private String productVersion;

  /** JDBC Driver 名称 */
  @Column(name = "driver_name", length = 256)
  private String driverName;

  /** JDBC Driver 版本 */
  @Column(name = "driver_version", length = 256)
  private String driverVersion;

  @Column(name = "fetched_at")
  private Instant fetchedAt;

  /** 创建时间 */
  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  protected DatabaseSchema() {}

  public static DatabaseSchema create(
      Long dataSourceId, String databaseName, String alias, String remarkCustom) {
    DatabaseSchema db = new DatabaseSchema();
    db.dataSourceId = dataSourceId;
    db.databaseName = databaseName;
    db.alias = alias;
    db.remarkCustom = remarkCustom;
    db.fetchedAt = HighDate.mockInstant();
    db.createdAt = HighDate.mockInstant();
    return db;
  }

  public void updateMetadata(
      String productName,
      String productVersion,
      String driverName,
      String driverVersion,
      Instant fetchedAt) {
    this.productName = productName;
    this.productVersion = productVersion;
    this.driverName = driverName;
    this.driverVersion = driverVersion;
    this.fetchedAt = fetchedAt;
  }

  public void updateBasicInfo(String alias, String remarkCustom) {
    this.alias = alias;
    this.remarkCustom = remarkCustom;
  }

  public void clearDataSource() {
    this.dataSourceId = null;
  }

  public void rebindDataSource(Long dataSourceId) {
    this.dataSourceId = dataSourceId;
  }
}

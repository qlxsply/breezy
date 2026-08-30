package com.corwin.schemaforge.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * 数据库结构快照聚合根
 *
 * @author Corwin 2026/2/24
 */
@Getter
@Entity
@Table(name = "sf_database_snapshot")
public class DatabaseSnapshot {

  @Id
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "managed_database_id", nullable = false)
  private Long managedDatabaseId;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "remark", length = 512)
  private String remark;

  @Column(name = "db_type", length = 64)
  private String dbType;

  @Column(name = "db_version", length = 128)
  private String dbVersion;

  @Column(name = "schema_name", length = 128)
  private String schemaName;

  @Column(name = "logical_file_id", nullable = false, length = 36)
  private String logicalFileId;

  @Column(name = "sql_logical_file_id", length = 36)
  private String sqlLogicalFileId;

  @Column(name = "content_hash", nullable = false, length = 128)
  private String contentHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected DatabaseSnapshot() {}

  public DatabaseSnapshot(
      Long managedDatabaseId,
      String name,
      String remark,
      String dbType,
      String dbVersion,
      String schemaName,
      String logicalFileId,
      String sqlLogicalFileId,
      String contentHash) {
    this.id = UUID.randomUUID().toString();
    this.managedDatabaseId =
        Objects.requireNonNull(managedDatabaseId, "managedDatabaseId required");
    this.name = Objects.requireNonNull(name, "name required");
    this.remark = remark;
    this.dbType = dbType;
    this.dbVersion = dbVersion;
    this.schemaName = schemaName;
    this.logicalFileId = Objects.requireNonNull(logicalFileId, "logicalFileId required");
    this.sqlLogicalFileId = Objects.requireNonNull(sqlLogicalFileId, "sqlLogicalFileId required");
    this.contentHash = Objects.requireNonNull(contentHash, "contentHash required");
    this.createdAt = HighDate.mockInstant();
  }

  public void updateInfo(String name, String remark) {
    this.name = Objects.requireNonNull(name, "name required");
    this.remark = remark;
  }
}

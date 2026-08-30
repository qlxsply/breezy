package com.corwin.datasource.infrastructure.jdbc;

import com.corwin.datasource.application.port.MetadataExtractResult;
import com.corwin.datasource.application.port.MetadataExtractor;
import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.framework.util.HighDate;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JDBC 元数据提取器实现
 *
 * @author Corwin 2026/1/11
 */
@Component
@RequiredArgsConstructor
public class JdbcMetadataExtractor implements MetadataExtractor {

  private final JdbcConnectionFactory connectionFactory;

  @Override
  public MetadataExtractResult.BaseInfo getBaseInfo(DatabaseSource dataSource) {
    try (Connection conn = connectionFactory.openConnection(dataSource)) {
      DatabaseMetaData meta = conn.getMetaData();
      return new MetadataExtractResult.BaseInfo(
          meta.getDatabaseProductName(),
          meta.getDatabaseProductVersion(),
          meta.getDriverName(),
          meta.getDriverVersion(),
          meta.getURL(),
          meta.getUserName(),
          HighDate.mockInstant());
    } catch (SQLException e) {
      throw new RuntimeException("Get base info failed: " + e.getMessage(), e);
    }
  }

  @Override
  public List<String> listDatabases(DatabaseSource dataSource) {
    List<String> databases = new ArrayList<>();
    try (Connection conn = connectionFactory.openConnection(dataSource)) {
      DatabaseMetaData meta = conn.getMetaData();
      if (dataSource.getDbType() == DatabaseType.MYSQL
          || dataSource.getDbType() == DatabaseType.SQLSERVER) {
        // 使用 Catalogs
        try (ResultSet rs = meta.getCatalogs()) {
          while (rs.next()) {
            databases.add(rs.getString("TABLE_CAT"));
          }
        }
      } else {
        // 使用 Schemas (Oracle, PostgreSQL)
        try (ResultSet rs = meta.getSchemas()) {
          while (rs.next()) {
            databases.add(rs.getString("TABLE_SCHEM"));
          }
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("List databases failed: " + e.getMessage(), e);
    }
    return databases;
  }

  @Override
  public MetadataExtractResult extract(DatabaseSource dataSource, String databaseName) {
    try (Connection conn = connectionFactory.openConnection(dataSource)) {
      DatabaseMetaData meta = conn.getMetaData();

      MetadataExtractResult.BaseInfo baseInfo =
          new MetadataExtractResult.BaseInfo(
              meta.getDatabaseProductName(),
              meta.getDatabaseProductVersion(),
              meta.getDriverName(),
              meta.getDriverVersion(),
              meta.getURL(),
              meta.getUserName(),
              HighDate.mockInstant());

      String catalog = null;
      String schema = null;
      if (dataSource.getDbType() == DatabaseType.MYSQL
          || dataSource.getDbType() == DatabaseType.SQLSERVER) {
        catalog = databaseName;
      } else {
        schema = databaseName;
      }

      // 表
      List<MetadataExtractResult.TableInfo> tables = loadTables(meta, catalog, schema);

      // 列
      List<MetadataExtractResult.ColumnInfo> columns = new ArrayList<>();
      for (MetadataExtractResult.TableInfo t : tables) {
        columns.addAll(loadColumns(meta, t.catalog(), t.schema(), t.name()));
      }

      return new MetadataExtractResult(baseInfo, tables, columns);

    } catch (SQLException e) {
      throw new RuntimeException("Extract metadata failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void testConnection(DatabaseSource dataSource) {
    try (Connection conn = connectionFactory.openConnection(dataSource)) {
      if (!conn.isValid(3)) {
        throw new RuntimeException("Connection is invalid");
      }
      conn.getMetaData().getDatabaseProductName();
    } catch (SQLException e) {
      throw new RuntimeException("Test connection failed: " + e.getMessage(), e);
    }
  }

  private List<MetadataExtractResult.TableInfo> loadTables(
      DatabaseMetaData meta, String catalog, String schema) throws SQLException {
    String[] types = new String[] {"TABLE", "VIEW"};
    List<MetadataExtractResult.TableInfo> list = new ArrayList<>();
    try (ResultSet rs = meta.getTables(catalog, schema, "%", types)) {
      while (rs.next()) {
        String tableCat = rs.getString("TABLE_CAT");
        String tableSchem = rs.getString("TABLE_SCHEM");
        String tableName = rs.getString("TABLE_NAME");
        String tableType = rs.getString("TABLE_TYPE");
        String remarks = rs.getString("REMARKS");
        list.add(
            new MetadataExtractResult.TableInfo(
                tableCat, tableSchem, tableName, tableType, remarks));
      }
    }
    return list;
  }

  private List<MetadataExtractResult.ColumnInfo> loadColumns(
      DatabaseMetaData meta, String catalog, String schema, String table) throws SQLException {
    List<MetadataExtractResult.ColumnInfo> list = new ArrayList<>();
    try (ResultSet rs = meta.getColumns(catalog, schema, table, "%")) {
      while (rs.next()) {
        // 严格按 JDBC 标准索引顺序读取，避免 Oracle 驱动对 LONG (COLUMN_DEF) 的流读取限制
        String tableCat = rs.getString("TABLE_CAT");
        String tableSchem = rs.getString("TABLE_SCHEM");
        String tableName = rs.getString("TABLE_NAME");
        String columnName = rs.getString("COLUMN_NAME");
        Integer dataType = getIntOrNull(rs, "DATA_TYPE");
        String typeName = rs.getString("TYPE_NAME");
        Integer columnSize = getIntOrNull(rs, "COLUMN_SIZE");
        Integer decimalDigits = getIntOrNull(rs, "DECIMAL_DIGITS");
        Integer nullableInt = getIntOrNull(rs, "NULLABLE");
        Boolean nullable =
            nullableInt == null ? null : (nullableInt != DatabaseMetaData.columnNoNulls);
        String remarks = rs.getString("REMARKS");
        String defaultValue = rs.getString("COLUMN_DEF");
        Integer ordinalPosition = getIntOrNull(rs, "ORDINAL_POSITION");

        list.add(
            new MetadataExtractResult.ColumnInfo(
                tableCat,
                tableSchem,
                tableName,
                columnName,
                typeName,
                dataType,
                columnSize,
                decimalDigits,
                nullable,
                ordinalPosition,
                defaultValue,
                remarks));
      }
    }
    return list;
  }

  private static Integer getIntOrNull(ResultSet rs, String col) throws SQLException {
    int v = rs.getInt(col);
    return rs.wasNull() ? null : v;
  }
}

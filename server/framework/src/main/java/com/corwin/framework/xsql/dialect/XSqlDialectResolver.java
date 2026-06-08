package com.corwin.framework.xsql.dialect;

import com.corwin.framework.xsql.error.XSqlDialectException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/**
 * XSql 方言识别器。
 * <p>
 * 根据 DataSource 实际连接的数据库产品名识别方言。
 * v1 仅允许 MySQL/MariaDB，其他数据库快速失败。
 *
 * @author Corwin 2026/4/9
 */
public class XSqlDialectResolver {

    /**
     * 解析数据源对应方言。
     *
     * @param dataSource 数据源
     * @return 方言实现
     */
    public XSqlDialect resolve(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String product = metaData.getDatabaseProductName();
            String normalized = product == null ? "" : product.toLowerCase(Locale.ROOT);
            if (normalized.contains("mysql") || normalized.contains("mariadb")) {
                return new MysqlXSqlDialect();
            }
            throw new XSqlDialectException("XSql only supports MySQL, current product is: " + product);
        } catch (SQLException e) {
            throw new XSqlDialectException("Resolve XSql dialect failed", e);
        }
    }
}


package com.corwin.framework.event.durable.store;

import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;

/**
 * Default {@link DatabaseVendorResolver} that inspects {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
 *
 * @author Corwin 2026/4/12
 */
public class DefaultDatabaseVendorResolver implements DatabaseVendorResolver {

    /**
     * 优先根据数据库产品名识别，必要时回退 JDBC URL。
     */
    @Override
    public DatabaseVendor resolve(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource required");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            DatabaseVendor byProduct = resolveByProductName(metaData.getDatabaseProductName());
            if (byProduct != DatabaseVendor.UNKNOWN) {
                return byProduct;
            }
            return resolveByJdbcUrl(metaData.getURL());
        } catch (SQLException ex) {
            throw new IllegalStateException("Resolve database vendor failed", ex);
        }
    }

    /**
     * 按数据库产品名识别。
     */
    private DatabaseVendor resolveByProductName(String productName) {
        if (!StringUtils.hasText(productName)) {
            return DatabaseVendor.UNKNOWN;
        }
        String normalized = productName.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("mysql") || normalized.contains("mariadb")) {
            return DatabaseVendor.MYSQL;
        }
        if (normalized.contains("postgresql")) {
            return DatabaseVendor.POSTGRESQL;
        }
        if (normalized.contains("oracle")) {
            return DatabaseVendor.ORACLE;
        }
        if (normalized.contains("sql server") || normalized.contains("microsoft sql server")) {
            return DatabaseVendor.SQLSERVER;
        }
        return DatabaseVendor.UNKNOWN;
    }

    /**
     * 按 JDBC URL 识别。
     */
    private DatabaseVendor resolveByJdbcUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return DatabaseVendor.UNKNOWN;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("jdbc:mysql:") || normalized.startsWith("jdbc:mariadb:")) {
            return DatabaseVendor.MYSQL;
        }
        if (normalized.startsWith("jdbc:postgresql:")) {
            return DatabaseVendor.POSTGRESQL;
        }
        if (normalized.startsWith("jdbc:oracle:")) {
            return DatabaseVendor.ORACLE;
        }
        if (normalized.startsWith("jdbc:sqlserver:")) {
            return DatabaseVendor.SQLSERVER;
        }
        return DatabaseVendor.UNKNOWN;
    }
}


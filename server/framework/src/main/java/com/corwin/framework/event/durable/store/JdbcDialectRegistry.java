package com.corwin.framework.event.durable.store;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JDBC 方言注册表。
 *
 * @author Corwin 2026/4/12
 */
public class JdbcDialectRegistry {

    private final Map<DatabaseVendor, JdbcDialect> dialects = new EnumMap<>(DatabaseVendor.class);

    public JdbcDialectRegistry(List<JdbcDialect> dialectList) {
        Objects.requireNonNull(dialectList, "dialectList required");
        for (JdbcDialect dialect : dialectList) {
            Objects.requireNonNull(dialect, "dialect required");
            dialects.put(dialect.vendor(), dialect);
        }
    }

    /**
     * 按 vendor 查找方言，不存在则抛出明确异常。
     */
    public JdbcDialect require(DatabaseVendor vendor) {
        JdbcDialect dialect = dialects.get(vendor);
        if (dialect != null) {
            return dialect;
        }
        throw new IllegalStateException(
                "Unsupported database vendor for async-event durable mode: " + vendor
                        + ". Current implementation supports: " + dialects.keySet());
    }
}


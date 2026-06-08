package com.corwin.framework.xsql;

import com.corwin.framework.xsql.dialect.XSqlDialect;
import com.corwin.framework.xsql.meta.XEntityMeta;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * XSql 默认会话实现。
 * <p>
 * 单个会话绑定一个数据源和对应方言，负责创建 DSL 查询对象。
 *
 * @author Corwin 2026/4/9
 */
final class DefaultXSqlSession implements XSqlSession {

    private final DefaultXSql xSql;
    private final DataSource dataSource;
    private final XSqlDialect dialect;

    DefaultXSqlSession(DefaultXSql xSql, DataSource dataSource, XSqlDialect dialect) {
        this.xSql = xSql;
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

    /**
     * 创建单表查询对象，并确保实体元数据已就绪。
     */
    @Override
    public <E, R> XTableQuery<E, R> table(Class<E> entityClass, Class<R> resultClass) {
        Objects.requireNonNull(entityClass, "entityClass must not be null");
        Objects.requireNonNull(resultClass, "resultClass must not be null");
        XEntityMeta meta = xSql.resolveEntityMeta(dataSource, entityClass, dialect);
        return new DefaultXTableQuery<>(dataSource, dialect, meta, resultClass);
    }

    /**
     * 创建原生 SQL 查询对象。
     */
    @Override
    public <R> XNativeQuery<R> nativeQuery(Class<R> resultClass) {
        Objects.requireNonNull(resultClass, "resultClass must not be null");
        return new DefaultXNativeQuery<>(dataSource, dialect, resultClass);
    }
}

package com.corwin.framework.xsql;

import com.corwin.framework.xsql.dialect.XSqlDialect;
import com.corwin.framework.xsql.dialect.XSqlDialectResolver;
import com.corwin.framework.xsql.meta.XEntityMeta;
import com.corwin.framework.xsql.meta.XEntityMetaKey;
import com.corwin.framework.xsql.meta.XEntityMetaParser;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * XSql 默认实现。
 * <p>
 * 该实现维护以下核心能力：
 * <ul>
 *     <li>方言解析器：按数据源识别数据库类型。</li>
 *     <li>实体元数据解析器：按需解析单表元数据。</li>
 *     <li>元数据缓存：避免重复反射解析。</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
final class DefaultXSql implements XSql {

    private final PhysicalNameResolver physicalNameResolver;
    private final XSqlDialectResolver dialectResolver;
    private final XEntityMetaParser entityMetaParser;
    private final ConcurrentMap<XEntityMetaKey, XEntityMeta> metaCache;

    private DefaultXSql(BuilderImpl builder) {
        this.physicalNameResolver = builder.physicalNameResolver == null ? new DefaultPhysicalNameResolver() :
                builder.physicalNameResolver;
        this.dialectResolver = new XSqlDialectResolver();
        this.entityMetaParser = new XEntityMetaParser(this.physicalNameResolver);
        this.metaCache = new ConcurrentHashMap<>();
    }

    /**
     * 创建内部构建器。
     */
    static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * 按数据源创建会话并解析对应方言。
     */
    @Override
    public XSqlSession using(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        XSqlDialect dialect = dialectResolver.resolve(dataSource);
        return new DefaultXSqlSession(this, dataSource, dialect);
    }

    /**
     * 解析并缓存实体元数据。
     * <p>
     * 缓存 Key 由数据源身份、实体类型、命名解析器身份共同决定。
     */
    XEntityMeta resolveEntityMeta(DataSource dataSource, Class<?> entityClass, XSqlDialect dialect) {
        XEntityMetaKey key = new XEntityMetaKey(System.identityHashCode(dataSource), entityClass,
                System.identityHashCode(physicalNameResolver));
        return metaCache.computeIfAbsent(key, unused -> entityMetaParser.parse(entityClass, dialect));
    }

    /**
     * 默认构建器实现。
     */
    private static class BuilderImpl implements Builder {

        private PhysicalNameResolver physicalNameResolver;

        /**
         * 指定物理命名解析器。
         */
        @Override
        public Builder physicalNameResolver(PhysicalNameResolver resolver) {
            this.physicalNameResolver = resolver;
            return this;
        }

        /**
         * 构建 DefaultXSql。
         */
        @Override
        public XSql build() {
            return new DefaultXSql(this);
        }
    }
}

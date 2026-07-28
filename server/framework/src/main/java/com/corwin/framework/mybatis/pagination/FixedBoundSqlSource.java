package com.corwin.framework.mybatis.pagination;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 固定返回指定 BoundSql 的 SqlSource。
 *
 * @author Corwin 2026/7/28
 */
public final class FixedBoundSqlSource implements SqlSource {

    private final BoundSql boundSql;

    public FixedBoundSqlSource(BoundSql boundSql) {
        this.boundSql = boundSql;
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        return boundSql;
    }
}

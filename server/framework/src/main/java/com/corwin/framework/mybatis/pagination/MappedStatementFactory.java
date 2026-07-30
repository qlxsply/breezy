package com.corwin.framework.mybatis.pagination;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlSource;

import java.util.List;

/**
 * Factory for creating copies of {@link org.apache.ibatis.mapping.MappedStatement}
 * with a different ID, {@link org.apache.ibatis.mapping.SqlSource}, and result maps.
 *
 * @author Corwin 2026/7/28
 */
public final class MappedStatementFactory {

    private MappedStatementFactory() {
    }

    public static MappedStatement copy(MappedStatement source, String id, SqlSource sqlSource,
            List<ResultMap> resultMaps) {
        MappedStatement.Builder builder = new MappedStatement.Builder(source.getConfiguration(), id, sqlSource,
                source.getSqlCommandType());

        builder.resource(source.getResource());
        builder.fetchSize(source.getFetchSize());
        builder.timeout(source.getTimeout());
        builder.statementType(source.getStatementType());
        builder.resultSetType(source.getResultSetType());
        builder.parameterMap(source.getParameterMap());
        builder.resultMaps(resultMaps);
        builder.cache(source.getCache());
        builder.flushCacheRequired(source.isFlushCacheRequired());
        builder.useCache(source.isUseCache());
        builder.resultOrdered(source.isResultOrdered());
        builder.databaseId(source.getDatabaseId());
        builder.lang(source.getLang());
        builder.dirtySelect(source.isDirtySelect());

        if (source.getKeyGenerator() != null) {
            builder.keyGenerator(source.getKeyGenerator());
        }

        if (source.getKeyProperties() != null && source.getKeyProperties().length > 0) {
            builder.keyProperty(String.join(",", source.getKeyProperties()));
        }

        if (source.getKeyColumns() != null && source.getKeyColumns().length > 0) {
            builder.keyColumn(String.join(",", source.getKeyColumns()));
        }

        if (source.getResultSets() != null && source.getResultSets().length > 0) {
            builder.resultSets(String.join(",", source.getResultSets()));
        }

        return builder.build();
    }
}

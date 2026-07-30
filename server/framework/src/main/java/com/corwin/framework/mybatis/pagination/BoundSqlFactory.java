package com.corwin.framework.mybatis.pagination;

import com.corwin.framework.mybatis.pagination.dialect.PageParameter;
import com.corwin.framework.mybatis.pagination.dialect.PageSql;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating count and page {@link org.apache.ibatis.mapping.BoundSql} instances
 * by cloning the original query's parameter mappings and additional parameters.
 *
 * @author Corwin 2026/7/28
 */
public final class BoundSqlFactory {

    private BoundSqlFactory() {
    }

    public static BoundSql forCount(Configuration configuration, BoundSql source, String countSql) {
        BoundSql target = new BoundSql(configuration, countSql, new ArrayList<>(source.getParameterMappings()),
                source.getParameterObject());

        copyAdditionalParameters(source, target);
        return target;
    }

    public static BoundSql forPage(Configuration configuration, BoundSql source, PageSql pageSql) {
        List<ParameterMapping> mappings = new ArrayList<>(source.getParameterMappings());

        for (PageParameter parameter : pageSql.parameters()) {
            mappings.add(new ParameterMapping.Builder(configuration, parameter.name(), parameter.javaType()).build());
        }

        BoundSql target = new BoundSql(configuration, pageSql.sql(), mappings, source.getParameterObject());

        copyAdditionalParameters(source, target);

        for (PageParameter parameter : pageSql.parameters()) {
            target.setAdditionalParameter(parameter.name(), parameter.value());
        }

        return target;
    }

    private static void copyAdditionalParameters(BoundSql source, BoundSql target) {
        for (Map.Entry<String, Object> entry : source.getAdditionalParameters().entrySet()) {
            target.setAdditionalParameter(entry.getKey(), entry.getValue());
        }
    }
}

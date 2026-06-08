package com.corwin.framework.xsql.support;

import com.corwin.framework.xsql.XSortDirection;

import java.util.List;

/**
 * XSql 排序子句构建器。
 * <p>
 * 负责将排序对象列表拼接为 ORDER BY 子句。
 *
 * @author Corwin 2026/4/9
 */
public final class XSqlOrderBuilder {

    private XSqlOrderBuilder() {
    }

    /**
     * 追加排序 SQL。
     *
     * @param sql 基础 SQL
     * @param orders 排序项
     * @return 拼接后的 SQL
     */
    public static String append(String sql, List<XSqlOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return sql;
        }
        StringBuilder builder = new StringBuilder(sql);
        builder.append(" order by ");
        int idx = 0;
        for (XSqlOrder order : orders) {
            if (idx++ > 0) {
                builder.append(", ");
            }
            builder.append(order.fieldExpr()).append(' ')
                    .append(order.direction() == XSortDirection.DESC ? "desc" : "asc");
        }
        return builder.toString();
    }
}


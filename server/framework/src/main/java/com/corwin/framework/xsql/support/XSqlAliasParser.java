package com.corwin.framework.xsql.support;

import com.corwin.framework.xsql.error.XSqlQueryBuildException;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 原生 SQL alias 解析器。
 * <p>
 * 用于从 SELECT SQL 中提取 {@code AS alias} 白名单，支撑原生查询排序与映射安全校验。
 *
 * @author Corwin 2026/4/9
 */
public final class XSqlAliasParser {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("(?i)\\bas\\s+([a-zA-Z_][a-zA-Z0-9_]*)");

    private XSqlAliasParser() {
    }

    /**
     * 解析 SQL 中所有 select alias（统一转小写）。
     */
    public static Set<String> parseSelectAliases(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new XSqlQueryBuildException("SQL template is blank");
        }
        Matcher matcher = ALIAS_PATTERN.matcher(sql);
        Set<String> aliases = new LinkedHashSet<>();
        while (matcher.find()) {
            String alias = matcher.group(1);
            String normalized = alias.toLowerCase(Locale.ROOT);
            if (!aliases.add(normalized)) {
                throw new XSqlQueryBuildException("Duplicate select alias in native SQL: " + alias);
            }
        }
        if (aliases.isEmpty()) {
            throw new XSqlQueryBuildException("Native SQL must contain explicit alias by AS");
        }
        return aliases;
    }

    /**
     * 粗略判断 SQL 是否已包含 where 关键字。
     */
    public static boolean containsWhereKeyword(String sql) {
        if (sql == null) {
            return false;
        }
        return sql.toLowerCase(Locale.ROOT).contains(" where ");
    }
}


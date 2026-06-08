package com.corwin.framework.xsql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定复杂查询别名与字段映射关系。
 * <p>
 * 在原生 SQL 结果映射时，若字段名与 SQL alias 不一致，可通过该注解显式声明。
 *
 * @author Corwin 2026/4/9
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface XColumn {

    /**
     * SQL 查询中的 alias 名称。
     */
    String value();
}


package com.corwin.framework.xsql;

import java.io.Serializable;
import java.util.function.Function;

/**
 * XSql 属性 Getter 函数式接口。
 * <p>
 * 用于 Lambda 属性路径解析（例如 {@code User::getName}），
 * 通过 {@link java.lang.invoke.SerializedLambda} 推导属性名，减少字符串硬编码。
 *
 * @author Corwin 2026/4/9
 */
@FunctionalInterface
public interface XGetter<T, R> extends Function<T, R>, Serializable {
}

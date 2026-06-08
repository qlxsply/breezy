package com.corwin.framework.xsql;

import com.corwin.framework.xsql.error.XSqlQueryBuildException;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lambda Getter 属性名解析器。
 * <p>
 * 将方法引用（如 {@code User::getName}）解析为属性路径（{@code name}），
 * 并使用缓存避免重复解析 SerializedLambda。
 *
 * @author Corwin 2026/4/9
 */
final class XLambdaPropertyResolver {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private XLambdaPropertyResolver() {
    }

    /**
     * 解析 getter Lambda 对应的属性名。
     */
    static String resolve(XGetter<?, ?> getter) {
        if (getter == null) {
            throw new XSqlQueryBuildException("Getter lambda must not be null");
        }
        SerializedLambda lambda = extractLambda(getter);
        String key = lambda.getImplClass() + "#" + lambda.getImplMethodName();
        return CACHE.computeIfAbsent(key, _unused -> parseProperty(lambda));
    }

    /**
     * 从 Lambda 对象中提取 SerializedLambda。
     */
    private static SerializedLambda extractLambda(XGetter<?, ?> getter) {
        try {
            Method method = getter.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            Object value = method.invoke(getter);
            if (value instanceof SerializedLambda lambda) {
                return lambda;
            }
            throw new XSqlQueryBuildException("Unsupported getter lambda: writeReplace does not return SerializedLambda");
        } catch (NoSuchMethodException ex) {
            throw new XSqlQueryBuildException(
                    "Unsupported getter lambda: writeReplace method not found, cause=" + ex.getMessage());
        } catch (ReflectiveOperationException ex) {
            throw new XSqlQueryBuildException("Resolve getter lambda failed, cause=" + ex.getMessage());
        }
    }

    /**
     * 根据实现方法名推导属性名。
     */
    private static String parseProperty(SerializedLambda lambda) {
        String methodName = lambda.getImplMethodName();
        String property = toPropertyName(methodName);
        if (property == null || property.isBlank()) {
            throw new XSqlQueryBuildException(
                    "Only getter method reference is supported, method=" + methodName + ", implClass=" +
                            lambda.getImplClass().replace('/', '.'));
        }
        return property;
    }

    /**
     * 将 getter 方法名转换为属性名。
     */
    private static String toPropertyName(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return null;
        }
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }
        return null;
    }

    /**
     * 首字母小写化，遵循 JavaBean 规则。
     */
    private static String decapitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() > 1 && Character.isUpperCase(value.charAt(1)) && Character.isUpperCase(value.charAt(0))) {
            return value;
        }
        char[] chars = value.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
}

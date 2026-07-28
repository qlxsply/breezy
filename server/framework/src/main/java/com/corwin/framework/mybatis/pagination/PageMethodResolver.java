package com.corwin.framework.mybatis.pagination;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import org.apache.ibatis.io.Resources;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析 MyBatis statement 对应 Mapper 方法是否需要 PageData 自动分页。
 *
 * @author Corwin 2026/7/28
 */
public final class PageMethodResolver {

    private final Map<String, PageMethodDescriptor> descriptorCache = new ConcurrentHashMap<>();

    public PageMethodDescriptor resolve(String statementId) {
        return descriptorCache.computeIfAbsent(statementId, this::inspect);
    }

    private PageMethodDescriptor inspect(String statementId) {
        int separator = statementId.lastIndexOf('.');

        if (separator <= 0 || separator == statementId.length() - 1) {
            return PageMethodDescriptor.ORDINARY;
        }

        String mapperClassName = statementId.substring(0, separator);
        String methodName = statementId.substring(separator + 1);

        try {
            Class<?> mapperClass = Resources.classForName(mapperClassName);

            List<Method> methods = Arrays.stream(mapperClass.getMethods())
                    .filter(method -> method.getName().equals(methodName)).filter(method -> !method.isBridge())
                    .filter(method -> !method.isSynthetic()).toList();

            /*
             * 根据项目约束，不允许重载。
             * 这里仍采用静默放行，避免分页组件改变普通 MyBatis 行为。
             */
            if (methods.size() != 1) {
                return PageMethodDescriptor.ORDINARY;
            }

            Method method = methods.getFirst();

            long pageSpecCount = Arrays.stream(method.getParameterTypes()).filter(PageSpec.class::equals).count();

            boolean returnsPageData = PageData.class.equals(method.getReturnType());

            return pageSpecCount == 1 && returnsPageData ? PageMethodDescriptor.PAGED : PageMethodDescriptor.ORDINARY;
        } catch (ClassNotFoundException | LinkageError ignored) {
            /*
             * XML namespace 不一定必须是 Mapper 接口。
             * 无法解析时作为普通 MyBatis statement 处理。
             */
            return PageMethodDescriptor.ORDINARY;
        }
    }
}

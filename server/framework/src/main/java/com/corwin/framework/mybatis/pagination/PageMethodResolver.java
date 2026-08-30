package com.corwin.framework.mybatis.pagination;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.ibatis.io.Resources;

/**
 * Resolves whether a MyBatis mapper method requires automatic {@link PageData} pagination.
 *
 * <p>A method qualifies if its return type is {@link PageData} and it accepts a {@link
 * com.corwin.framework.domain.page.PageSpec PageSpec} parameter. Results are cached by statement
 * ID.
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

      List<Method> methods =
          Arrays.stream(mapperClass.getMethods())
              .filter(method -> method.getName().equals(methodName))
              .filter(method -> !method.isBridge())
              .filter(method -> !method.isSynthetic())
              .toList();

      /*
       * Method overloading is not permitted per project convention.
       * Silently skip to avoid interfering with ordinary MyBatis behavior.
       */
      if (methods.size() != 1) {
        return PageMethodDescriptor.ORDINARY;
      }

      Method method = methods.getFirst();

      long pageSpecCount =
          Arrays.stream(method.getParameterTypes()).filter(PageSpec.class::equals).count();

      boolean returnsPageData = PageData.class.equals(method.getReturnType());

      return pageSpecCount == 1 && returnsPageData
          ? PageMethodDescriptor.PAGED
          : PageMethodDescriptor.ORDINARY;
    } catch (ClassNotFoundException | LinkageError ignored) {
      /*
       * An XML namespace may not always correspond to a Mapper interface.
       * Treat unresolvable statements as ordinary ones.
       */
      return PageMethodDescriptor.ORDINARY;
    }
  }
}

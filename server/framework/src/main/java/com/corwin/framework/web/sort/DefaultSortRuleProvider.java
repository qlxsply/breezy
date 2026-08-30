package com.corwin.framework.web.sort;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * Default {@link SortRuleProvider} that disables sorting for all endpoints.
 *
 * <p>Serves as the fallback when no custom provider is registered.
 *
 * @author Corwin 2026/7/29
 */
public class DefaultSortRuleProvider implements SortRuleProvider {

  @Override
  public SortRule getRule(HttpServletRequest request, HandlerMethod handlerMethod) {
    return SortRule.disabled();
  }
}

package com.corwin.framework.web.sort;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * Strategy interface for providing sort rules per endpoint.
 *
 * <p>Implementations determine which sort fields are available and what the default sort order is
 * for a given request and handler method.
 *
 * @author Corwin 2026/7/29
 */
public interface SortRuleProvider {

  SortRule getRule(HttpServletRequest request, HandlerMethod handlerMethod);
}

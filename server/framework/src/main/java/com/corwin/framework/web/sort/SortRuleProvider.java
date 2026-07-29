package com.corwin.framework.web.sort;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * 接口排序规则提供者。
 *
 * @author Corwin 2026/7/29
 */
public interface SortRuleProvider {

    SortRule getRule(HttpServletRequest request, HandlerMethod handlerMethod);
}

package com.corwin.framework.web.sort;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.method.HandlerMethod;

/**
 * 默认排序规则提供者：所有接口默认不支持排序。
 *
 * @author Corwin 2026/7/29
 */
public class DefaultSortRuleProvider implements SortRuleProvider {

    @Override
    public SortRule getRule(HttpServletRequest request, HandlerMethod handlerMethod) {
        return SortRule.disabled();
    }
}

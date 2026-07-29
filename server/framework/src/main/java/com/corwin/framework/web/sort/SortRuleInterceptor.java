package com.corwin.framework.web.sort;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求级排序规则注入拦截器。
 *
 * @author Corwin 2026/7/29
 */
public class SortRuleInterceptor implements HandlerInterceptor {

    private final SortRuleProvider provider;

    public SortRuleInterceptor(SortRuleProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            SortRuleContextHolder.set(provider.getRule(request, handlerMethod));
        } else {
            SortRuleContextHolder.set(SortRule.disabled());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        SortRuleContextHolder.clear();
    }
}

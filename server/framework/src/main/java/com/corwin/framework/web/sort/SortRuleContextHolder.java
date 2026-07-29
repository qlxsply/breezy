package com.corwin.framework.web.sort;

/**
 * 当前请求排序规则上下文。
 *
 * @author Corwin 2026/7/29
 */
public final class SortRuleContextHolder {

    private static final ThreadLocal<SortRule> HOLDER = new ThreadLocal<>();

    private SortRuleContextHolder() {
    }

    public static void set(SortRule rule) {
        HOLDER.set(rule);
    }

    public static SortRule get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

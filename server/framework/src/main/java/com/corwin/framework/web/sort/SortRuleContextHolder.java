package com.corwin.framework.web.sort;

/**
 * Thread-local holder for the current request's {@link SortRule}.
 *
 * <p>The rule is set by {@link SortRuleInterceptor} during pre-handle and automatically cleared
 * after request completion.
 *
 * @author Corwin 2026/7/29
 */
public final class SortRuleContextHolder {

  private static final ThreadLocal<SortRule> HOLDER = new ThreadLocal<>();

  private SortRuleContextHolder() {}

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

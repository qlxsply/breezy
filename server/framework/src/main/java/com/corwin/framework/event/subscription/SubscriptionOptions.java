package com.corwin.framework.event.subscription;

import java.util.List;
import java.util.Objects;

/**
 * 订阅可选配置。
 *
 * @author Corwin 2026/4/9
 */
public record SubscriptionOptions(
        List<String> sources,
        int order,
        boolean allowSharedGroup
) {

    /**
     * 归一化来源过滤项，去除空白并去重。
     */
    public SubscriptionOptions {
        if (sources == null || sources.isEmpty()) {
            sources = List.of();
        } else {
            sources = sources.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isEmpty())
                    .distinct().toList();
        }
    }
}


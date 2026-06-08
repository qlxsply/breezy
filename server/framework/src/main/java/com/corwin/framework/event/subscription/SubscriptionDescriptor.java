package com.corwin.framework.event.subscription;

import com.corwin.framework.event.model.AsyncEvent;

import java.util.Objects;

/**
 * 逻辑订阅者描述。
 * <p>
 * 该对象是 V2 模型中的核心单元，transport 会围绕它进行消费拓扑构建。
 * 一个监听方法通常对应一个 SubscriptionDescriptor。
 *
 * @author Corwin 2026/4/9
 */
public record SubscriptionDescriptor(
        String subscriptionId,
        String subscriberName,
        String consumerGroupRaw,
        String consumerGroup,
        Class<? extends AsyncEvent> eventClass,
        String eventType,
        SubscriberInvoker invoker,
        SubscriptionMode mode,
        SubscriptionOptions options
) {

    /**
     * 校验并补齐订阅描述默认值。
     */
    public SubscriptionDescriptor {
        Objects.requireNonNull(subscriptionId, "subscriptionId required");
        Objects.requireNonNull(subscriberName, "subscriberName required");
        Objects.requireNonNull(consumerGroupRaw, "consumerGroupRaw required");
        Objects.requireNonNull(consumerGroup, "consumerGroup required");
        Objects.requireNonNull(eventClass, "eventClass required");
        Objects.requireNonNull(eventType, "eventType required");
        Objects.requireNonNull(invoker, "invoker required");
        if (mode == null) {
            mode = SubscriptionMode.DEFAULT;
        }
        if (options == null) {
            options = new SubscriptionOptions(null, 0, false);
        }
    }
}


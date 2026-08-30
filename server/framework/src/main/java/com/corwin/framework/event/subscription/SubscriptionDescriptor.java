package com.corwin.framework.event.subscription;

import com.corwin.framework.event.model.AsyncEvent;
import java.util.Objects;

/**
 * Describes a logical subscriber — the core unit of the V2 event model.
 *
 * <p>Each annotated listener method typically maps to one {@code SubscriptionDescriptor}, and
 * transports build their consumption topology around it.
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
    SubscriptionOptions options) {

  /** 校验并补齐订阅描述默认值。 */
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

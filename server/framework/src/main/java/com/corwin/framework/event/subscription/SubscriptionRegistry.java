package com.corwin.framework.event.subscription;

import java.util.List;

/**
 * Registry maintaining the mapping from event type to subscriber collection.
 * <p>
 * Queried by transports during startup and publish phases.
 *
 * @author Corwin 2026/4/9
 */
public interface SubscriptionRegistry {

    /**
     * 注册订阅描述。
     *
     * @param descriptor 订阅描述
     */
    void register(SubscriptionDescriptor descriptor);

    /**
     * 按事件类型查询订阅列表。
     *
     * @param eventType 事件类型全限定名
     * @return 订阅列表
     */
    List<SubscriptionDescriptor> getByEventType(String eventType);

    /**
     * 获取全部订阅描述。
     *
     * @return 全量订阅列表
     */
    List<SubscriptionDescriptor> getAll();
}


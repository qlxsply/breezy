package com.corwin.framework.event.subscription;

import java.util.List;

/**
 * 订阅注册表。
 * <p>
 * 统一维护“事件类型 -> 订阅者集合”的映射，供 transport 在启动和发布阶段查询。
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


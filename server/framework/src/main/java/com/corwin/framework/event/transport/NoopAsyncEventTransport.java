package com.corwin.framework.event.transport;

import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.subscription.SubscriptionRegistry;

/**
 * 空操作 transport。
 * <p>
 * 在事件组件禁用时作为占位实现，所有方法均无副作用。
 *
 * @author Corwin 2026/4/9
 */
public class NoopAsyncEventTransport implements AsyncEventTransport {

    /**
     * 空实现：忽略启动。
     */
    @Override
    public void start(SubscriptionRegistry registry) {
    }

    /**
     * 空实现：忽略发布。
     */
    @Override
    public void publish(AsyncEventEnvelope<?> envelope) {
    }

    /**
     * 空实现：忽略关闭。
     */
    @Override
    public void shutdown() {
    }
}


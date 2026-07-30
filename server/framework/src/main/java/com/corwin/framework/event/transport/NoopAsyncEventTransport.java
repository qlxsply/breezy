package com.corwin.framework.event.transport;

import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.subscription.SubscriptionRegistry;

/**
 * No-op transport used when the event bus is disabled.
 * <p>
 * All operations are side-effect-free, acting as a Null Object replacement.
 *
 * @author Corwin 2026/3/31
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


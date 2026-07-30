package com.corwin.framework.event.subscription;

import com.corwin.framework.event.model.AsyncEventEnvelope;

/**
 * Abstraction for invoking a subscriber — regardless of whether it was registered
 * via annotation or another mechanism. Transports depend only on this interface
 * and do not need to know about reflection or bean invocation details.
 *
 * @author Corwin 2026/4/9
 */
public interface SubscriberInvoker {

    /**
     * 执行单次订阅消费。
     *
     * @param envelope 事件信封
     * @throws Exception 监听方法异常，交由 transport 决定失败策略
     */
    void invoke(AsyncEventEnvelope<?> envelope) throws Exception;
}


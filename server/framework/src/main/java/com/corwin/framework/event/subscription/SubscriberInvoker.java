package com.corwin.framework.event.subscription;

import com.corwin.framework.event.model.AsyncEventEnvelope;

/**
 * 订阅者调用抽象。
 * <p>
 * 无论监听来源是注解方法还是其他适配方式，最终都收敛为该调用接口，
 * 使 transport 只关注“如何投递”，而不关心“如何反射调用业务代码”。
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


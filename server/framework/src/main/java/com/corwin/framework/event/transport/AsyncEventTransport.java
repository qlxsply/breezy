package com.corwin.framework.event.transport;

import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.subscription.SubscriptionRegistry;

/**
 * 异步事件传输抽象。
 * <p>
 * 定义统一生命周期：启动绑定订阅、发布事件、优雅关闭。
 * 不同实现可对应内存分发或外部消息中间件。
 *
 * @author Corwin 2026/3/31
 */
public interface AsyncEventTransport {

    /**
     * 启动 transport 并加载订阅关系。
     *
     * @param registry 订阅注册表
     */
    void start(SubscriptionRegistry registry);

    /**
     * 发布事件信封。
     *
     * @param envelope 事件信封
     */
    void publish(AsyncEventEnvelope<?> envelope);

    /**
     * 关闭 transport 并释放资源。
     */
    void shutdown();
}

package com.corwin.framework.event.transport;

import com.corwin.framework.event.config.AsyncEventProperties;
import com.corwin.framework.event.subscription.SubscriptionRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.Objects;

/**
 * 事件传输组件生命周期桥接器。
 * <p>
 * 通过 Spring 生命周期回调统一控制 transport 启停时序：
 * 容器单例完成后再启动，容器销毁时执行关闭。
 *
 * @author Corwin 2026/4/9
 */
public class AsyncEventTransportLifecycle implements SmartInitializingSingleton, DisposableBean {

    private final AsyncEventProperties properties;
    private final AsyncEventTransport transport;
    private final SubscriptionRegistry registry;

    public AsyncEventTransportLifecycle(AsyncEventProperties properties, AsyncEventTransport transport,
            SubscriptionRegistry registry) {
        this.properties = Objects.requireNonNull(properties, "properties required");
        this.transport = Objects.requireNonNull(transport, "transport required");
        this.registry = Objects.requireNonNull(registry, "registry required");
    }

    /**
     * 所有单例初始化完成后启动 transport，确保订阅扫描结果完整。
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled()) {
            return;
        }
        transport.start(registry);
    }

    /**
     * Bean 销毁阶段关闭 transport。
     */
    @Override
    public void destroy() {
        transport.shutdown();
    }
}


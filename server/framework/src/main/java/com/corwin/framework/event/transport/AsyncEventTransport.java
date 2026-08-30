package com.corwin.framework.event.transport;

import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.subscription.SubscriptionRegistry;

/**
 * Abstraction for async event transport.
 *
 * <p>Defines a unified lifecycle: bind subscriptions on start, publish events, and gracefully shut
 * down. Implementations span in-memory dispatch and external message brokers.
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

  /** 关闭 transport 并释放资源。 */
  void shutdown();
}

package com.corwin.framework.event.transport.inmemory;

import com.corwin.framework.event.context.ConsumeContextBinder;
import com.corwin.framework.event.context.ConsumeContextScope;
import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.subscription.SubscriptionDescriptor;
import com.corwin.framework.event.subscription.SubscriptionRegistry;
import com.corwin.framework.event.transport.AsyncEventTransport;
import com.corwin.framework.event.transport.inmemory.executor.InMemoryDispatchExecutor;
import com.corwin.framework.util.HighDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * In-memory async event transport using a per-subscriber dispatch model.
 *
 * <p>On publish, queries all subscriptions by event type and dispatches to each subscriber
 * independently. Used for intra-process async decoupling while keeping subscription semantics
 * consistent with MQ-based transports.
 *
 * @author Corwin 2026/4/9
 */
@Slf4j
public class InMemoryAsyncEventTransport implements AsyncEventTransport {

  private static final Comparator<DelayedDispatchTask> DELAY_TASK_COMPARATOR =
      Comparator.comparingLong(DelayedDispatchTask::deliverAtMillis)
          .thenComparingLong(DelayedDispatchTask::sequence);

  private final InMemoryDispatchExecutor executor;
  private final ConsumeContextBinder contextBinder;
  private final DelayQueue<DelayedDispatchTask> delayQueue = new DelayQueue<>();
  private final AtomicLong delayTaskSequence = new AtomicLong(0L);

  private final AtomicBoolean started = new AtomicBoolean(false);
  private SubscriptionRegistry registry;
  private Thread delayWorker;

  public InMemoryAsyncEventTransport(
      InMemoryDispatchExecutor executor, ConsumeContextBinder contextBinder) {
    this.executor = Objects.requireNonNull(executor, "executor required");
    this.contextBinder = Objects.requireNonNull(contextBinder, "contextBinder required");
  }

  /** 启动 transport 并绑定订阅注册表。 */
  @Override
  public void start(SubscriptionRegistry registry) {
    Objects.requireNonNull(registry, "registry required");
    if (started.compareAndSet(false, true)) {
      this.registry = registry;
      executor.start();
      startDelayWorker();
      log.info("In-memory async event transport started");
    }
  }

  /** 发布事件并对匹配订阅者逐个投递。 */
  @Override
  public void publish(AsyncEventEnvelope<?> envelope) {
    Objects.requireNonNull(envelope, "envelope required");
    if (!started.get() || registry == null) {
      throw new IllegalStateException("In-memory async event transport is not started");
    }
    List<SubscriptionDescriptor> subscriptions = registry.getByEventType(envelope.eventType());
    if (subscriptions.isEmpty()) {
      return;
    }
    for (SubscriptionDescriptor subscription : subscriptions) {
      if (!matchesSource(subscription, envelope.source())) {
        continue;
      }
      submitDelivery(subscription, envelope);
    }
  }

  /** 停止分发并释放执行器资源。 */
  @Override
  public void shutdown() {
    if (started.compareAndSet(true, false)) {
      stopDelayWorker();
      delayQueue.clear();
      executor.shutdown();
      registry = null;
      log.info("In-memory async event transport stopped");
    }
  }

  /** 按 deliverAtMillis 判断立即投递或进入延时队列。 */
  private void submitDelivery(SubscriptionDescriptor subscription, AsyncEventEnvelope<?> envelope) {
    long now = HighDate.realTimestampMillis();
    if (envelope.deliverAtMillis() <= now) {
      executor.execute(() -> invokeSubscriber(subscription, envelope));
      return;
    }
    delayQueue.offer(
        new DelayedDispatchTask(
            subscription,
            envelope,
            envelope.deliverAtMillis(),
            delayTaskSequence.incrementAndGet()));
  }

  /** 启动延时任务搬运线程：只负责“到点后转入 ready 执行器”。 */
  private void startDelayWorker() {
    Thread worker = new Thread(this::runDelayWorker, "async-event-delay-worker");
    worker.setDaemon(true);
    this.delayWorker = worker;
    worker.start();
  }

  /** 停止延时任务搬运线程并等待短暂退出。 */
  private void stopDelayWorker() {
    Thread worker = this.delayWorker;
    this.delayWorker = null;
    if (worker == null) {
      return;
    }
    worker.interrupt();
    try {
      worker.join(1000L);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  /** 搬运线程主循环：从 DelayQueue 取到期任务并提交到执行器。 */
  private void runDelayWorker() {
    while (started.get()) {
      try {
        DelayedDispatchTask task = delayQueue.take();
        if (!started.get()) {
          return;
        }
        executor.execute(() -> invokeSubscriber(task.subscription(), task.envelope()));
      } catch (InterruptedException ex) {
        if (!started.get()) {
          Thread.currentThread().interrupt();
          return;
        }
      } catch (Exception ex) {
        log.warn("Delay worker submit task failed", ex);
      }
    }
  }

  /** 判断事件来源是否命中订阅的 source 白名单。 */
  private boolean matchesSource(SubscriptionDescriptor subscription, String source) {
    List<String> sources = subscription.options().sources();
    if (sources.isEmpty()) {
      return true;
    }
    if (source == null || source.isBlank()) {
      return false;
    }
    return sources.contains(source.trim());
  }

  /** 执行单个订阅消费，消费前恢复上下文，失败统一记录结构化日志。 */
  private void invokeSubscriber(
      SubscriptionDescriptor subscription, AsyncEventEnvelope<?> envelope) {
    try (ConsumeContextScope ignored = contextBinder.bind(envelope.ctxSnapshot())) {
      subscription.invoker().invoke(envelope);
    } catch (Exception ex) {
      log.warn(
          "Async event consume failed. eventId={}, eventType={}, subscriptionId={}, group={}, subscriber={}",
          envelope.eventId(),
          envelope.eventType(),
          subscription.subscriptionId(),
          subscription.consumerGroup(),
          subscription.subscriberName(),
          ex);
    }
  }

  /** 延时队列元素：持有订阅信息与事件信封，在到期后进入 ready 执行器。 */
  private record DelayedDispatchTask(
      SubscriptionDescriptor subscription,
      AsyncEventEnvelope<?> envelope,
      long deliverAtMillis,
      long sequence)
      implements Delayed {

    /** 返回距离可执行时间点的剩余延迟。 */
    @Override
    public long getDelay(TimeUnit unit) {
      long delayMillis = deliverAtMillis - HighDate.realTimestampMillis();
      return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
    }

    /** 按到期时间与入队顺序排序，保证同时间点下顺序稳定。 */
    @Override
    public int compareTo(Delayed other) {
      if (other == this) {
        return 0;
      }
      if (other instanceof DelayedDispatchTask task) {
        return DELAY_TASK_COMPARATOR.compare(this, task);
      }
      long thisDelay = getDelay(TimeUnit.MILLISECONDS);
      long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);
      return Long.compare(thisDelay, otherDelay);
    }
  }
}

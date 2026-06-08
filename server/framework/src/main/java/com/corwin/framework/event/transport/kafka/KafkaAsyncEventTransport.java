package com.corwin.framework.event.transport.kafka;

import com.corwin.framework.event.context.ConsumeContextBinder;
import com.corwin.framework.event.context.ConsumeContextScope;
import com.corwin.framework.event.config.AsyncEventProperties;
import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.serialize.EventSerializer;
import com.corwin.framework.event.subscription.SubscriptionDescriptor;
import com.corwin.framework.event.subscription.SubscriptionRegistry;
import com.corwin.framework.event.transport.AsyncEventTransport;
import com.corwin.framework.event.transport.inmemory.executor.InMemoryDispatchExecutor;
import com.corwin.framework.event.transport.inmemory.executor.ThreadPoolInMemoryDispatchExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka transport（当前为本地分发语义实现）。
 * <p>
 * 当前模块未引入真实 Kafka 客户端依赖时，该实现通过“序列化 + 反序列化 + 本地执行器”
 * 模拟跨边界传输链路，优先验证订阅模型、来源过滤、上下文恢复与异常日志结构。
 * 后续接入真实 Kafka 时可沿用同一抽象边界。
 *
 * @author Corwin 2026/4/9
 */
@Slf4j
public class KafkaAsyncEventTransport implements AsyncEventTransport {

    private final EventTopicResolver topicResolver;
    private final EventSerializer serializer;
    private final InMemoryDispatchExecutor executor;
    private final ConsumeContextBinder contextBinder;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private SubscriptionRegistry registry;

    public KafkaAsyncEventTransport(AsyncEventProperties properties, EventTopicResolver topicResolver,
            EventSerializer serializer, ConsumeContextBinder contextBinder) {
        Objects.requireNonNull(properties, "properties required");
        this.topicResolver = Objects.requireNonNull(topicResolver, "topicResolver required");
        this.serializer = Objects.requireNonNull(serializer, "serializer required");
        this.contextBinder = Objects.requireNonNull(contextBinder, "contextBinder required");
        this.executor = new ThreadPoolInMemoryDispatchExecutor(properties);
    }

    /**
     * 启动 transport 并初始化本地执行器。
     */
    @Override
    public void start(SubscriptionRegistry registry) {
        Objects.requireNonNull(registry, "registry required");
        if (started.compareAndSet(false, true)) {
            this.registry = registry;
            executor.start();
            log.warn(
                    "Kafka async-event transport is running in local-dispatch mode (no external broker integration module found).");
        }
    }

    /**
     * 发布事件：解析 topic，执行序列化往返，并按订阅者投递。
     */
    @Override
    public void publish(AsyncEventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope required");
        if (!started.get() || registry == null) {
            throw new IllegalStateException("Kafka async-event transport is not started");
        }
        String topic = topicResolver.resolveTopic(envelope.eventType());
        byte[] bytes = serializer.serialize(envelope);
        AsyncEventEnvelope<?> consumedEnvelope = serializer.deserialize(bytes);
        List<SubscriptionDescriptor> subscriptions = registry.getByEventType(consumedEnvelope.eventType());
        for (SubscriptionDescriptor subscription : subscriptions) {
            if (!matchesSource(subscription, consumedEnvelope.source())) {
                continue;
            }
            executor.execute(() -> invoke(subscription, consumedEnvelope, topic));
        }
    }

    /**
     * 停止 transport 并关闭执行器。
     */
    @Override
    public void shutdown() {
        if (started.compareAndSet(true, false)) {
            executor.shutdown();
            registry = null;
        }
    }

    /**
     * 判断事件来源是否命中订阅白名单。
     */
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

    /**
     * 执行订阅消费并在异常时输出包含 topic 的诊断日志。
     */
    private void invoke(SubscriptionDescriptor subscription, AsyncEventEnvelope<?> envelope, String topic) {
        try (ConsumeContextScope ignored = contextBinder.bind(envelope.ctxSnapshot())) {
            subscription.invoker().invoke(envelope);
        } catch (Exception ex) {
            log.warn(
                    "Kafka local-dispatch consume failed. topic={}, eventId={}, eventType={}, subscriptionId={}, group={}",
                    topic, envelope.eventId(), envelope.eventType(), subscription.subscriptionId(),
                    subscription.consumerGroup(), ex);
        }
    }
}

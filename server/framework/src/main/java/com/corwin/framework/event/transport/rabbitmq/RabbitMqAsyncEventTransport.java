package com.corwin.framework.event.transport.rabbitmq;

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
 * Local-dispatch RabbitMQ transport (simulation mode).
 * <p>
 * Parses exchange/routingKey/queue naming and performs serialisation round-trips,
 * then simulates the consumption path via a local executor — all without an
 * external broker. This validates subscription semantics and context restoration.
 *
 * @author Corwin 2026/4/9
 */
@Slf4j
public class RabbitMqAsyncEventTransport implements AsyncEventTransport {

    private final AsyncEventProperties properties;
    private final EventSerializer serializer;
    private final InMemoryDispatchExecutor executor;
    private final ConsumeContextBinder contextBinder;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private SubscriptionRegistry registry;

    public RabbitMqAsyncEventTransport(AsyncEventProperties properties, EventSerializer serializer,
            ConsumeContextBinder contextBinder) {
        this.properties = Objects.requireNonNull(properties, "properties required");
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
                    "RabbitMQ async-event transport is running in local-dispatch mode (no external broker integration module found).");
        }
    }

    /**
     * 发布事件并按订阅者进行本地模拟投递。
     */
    @Override
    public void publish(AsyncEventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope required");
        if (!started.get() || registry == null) {
            throw new IllegalStateException("RabbitMQ async-event transport is not started");
        }
        String exchange = resolveExchange();
        String routingKey = resolveRoutingKey(envelope.eventType());
        byte[] bytes = serializer.serialize(envelope);
        AsyncEventEnvelope<?> consumedEnvelope = serializer.deserialize(bytes);
        List<SubscriptionDescriptor> subscriptions = registry.getByEventType(consumedEnvelope.eventType());
        for (SubscriptionDescriptor subscription : subscriptions) {
            if (!matchesSource(subscription, consumedEnvelope.source())) {
                continue;
            }
            String queue = subscription.consumerGroup();
            executor.execute(() -> invoke(subscription, consumedEnvelope, exchange, routingKey, queue));
        }
    }

    /**
     * 停止 transport 并释放资源。
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
     * 解析 RabbitMQ exchange，缺省回退到统一默认值。
     */
    private String resolveExchange() {
        String exchange = properties.getRabbitmq().getExchange();
        return (exchange == null || exchange.isBlank()) ? "framework.async.event" : exchange.trim();
    }

    /**
     * 解析 routing key（可选前缀 + 事件类型）。
     */
    private String resolveRoutingKey(String eventType) {
        String prefix = properties.getRabbitmq().getRoutingKeyPrefix();
        String normalizedPrefix = (prefix == null || prefix.isBlank()) ? "" : prefix.trim() + ".";
        return normalizedPrefix + eventType;
    }

    /**
     * 执行订阅消费并记录包含 exchange/routing/queue 的诊断日志。
     */
    private void invoke(SubscriptionDescriptor subscription, AsyncEventEnvelope<?> envelope, String exchange,
            String routingKey, String queue) {
        try (ConsumeContextScope ignored = contextBinder.bind(envelope.ctxSnapshot())) {
            subscription.invoker().invoke(envelope);
        } catch (Exception ex) {
            log.warn(
                    "RabbitMQ local-dispatch consume failed. exchange={}, routingKey={}, queue={}, eventId={}, eventType={}, subscriptionId={}",
                    exchange, routingKey, queue, envelope.eventId(), envelope.eventType(), subscription.subscriptionId(),
                    ex);
        }
    }
}

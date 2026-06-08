package com.corwin.framework.event.publisher;

import com.corwin.framework.event.model.AsyncEvent;
import com.corwin.framework.event.transport.AsyncEventTransport;
import com.corwin.framework.util.HighDate;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 默认事件发布器。
 * <p>
 * 该实现将发布流程拆成两步：
 * 先通过 {@link AsyncEventEnvelopeFactory} 构建统一信封，
 * 再交由 {@link AsyncEventTransport} 执行实际投递。
 * 这样业务层不需要感知上下文快照、事件元数据与底层传输细节。
 *
 * @author Corwin 2026/3/31
 */
public class DefaultAsyncEventPublisher implements AsyncEventPublisher {

    private final AsyncEventEnvelopeFactory envelopeFactory;
    private final AsyncEventTransport transport;

    public DefaultAsyncEventPublisher(AsyncEventEnvelopeFactory envelopeFactory, AsyncEventTransport transport) {
        this.envelopeFactory = Objects.requireNonNull(envelopeFactory, "envelopeFactory required");
        this.transport = Objects.requireNonNull(transport, "transport required");
    }

    /**
     * 使用默认来源发布事件。
     */
    @Override
    public <T extends AsyncEvent> void publish(T event) {
        publish(event, null);
    }

    /**
     * 使用指定来源发布事件。
     */
    @Override
    public <T extends AsyncEvent> void publish(T event, String source) {
        Objects.requireNonNull(event, "event required");
        transport.publish(envelopeFactory.create(event, source));
    }

    /**
     * 使用绝对时间发布延时事件。
     */
    @Override
    public <T extends AsyncEvent> void publishAt(T event, Instant deliverAt) {
        publishAt(event, deliverAt, null);
    }

    /**
     * 使用绝对时间与显式来源发布延时事件。
     */
    @Override
    public <T extends AsyncEvent> void publishAt(T event, Instant deliverAt, String source) {
        Objects.requireNonNull(event, "event required");
        Objects.requireNonNull(deliverAt, "deliverAt required");
        transport.publish(envelopeFactory.create(event, source, deliverAt.toEpochMilli()));
    }

    /**
     * 延后指定时长发布事件。
     */
    @Override
    public <T extends AsyncEvent> void publishAfter(T event, Duration delay) {
        publishAfter(event, delay, null);
    }

    /**
     * 延后指定时长并显式来源发布事件。
     */
    @Override
    public <T extends AsyncEvent> void publishAfter(T event, Duration delay, String source) {
        Objects.requireNonNull(event, "event required");
        Objects.requireNonNull(delay, "delay required");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must be >= 0");
        }
        long delayMillis = delay.toMillis();
        long now = HighDate.mockTimestampMillis();
        long deliverAtMillis;
        try {
            deliverAtMillis = Math.addExact(now, delayMillis);
        } catch (ArithmeticException ex) {
            deliverAtMillis = Long.MAX_VALUE;
        }
        transport.publish(envelopeFactory.create(event, source, deliverAtMillis));
    }
}

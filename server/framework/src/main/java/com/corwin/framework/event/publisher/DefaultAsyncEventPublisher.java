package com.corwin.framework.event.publisher;

import com.corwin.framework.event.model.AsyncEvent;
import com.corwin.framework.event.transport.AsyncEventTransport;
import com.corwin.framework.util.HighDate;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Default {@link AsyncEventPublisher} that splits publishing into two steps:
 * envelope creation via {@link AsyncEventEnvelopeFactory} and delivery via
 * {@link AsyncEventTransport}. Business code is isolated from context management
 * and transport details.
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
        long now = HighDate.realTimestampMillis();
        long deliverAtMillis;
        try {
            deliverAtMillis = Math.addExact(now, delayMillis);
        } catch (ArithmeticException ex) {
            deliverAtMillis = Long.MAX_VALUE;
        }
        transport.publish(envelopeFactory.create(event, source, deliverAtMillis));
    }
}

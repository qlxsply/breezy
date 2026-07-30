package com.corwin.framework.event.model;

import java.util.Map;
import java.util.Objects;

/**
 * Unified envelope model for async event publish and transport.
 * <p>
 * Carries the event ID, type, occurrence time, context snapshot, payload,
 * partition key, and transport extension headers. This model is the neutral
 * carrier regardless of the underlying transport (in-memory, Kafka, RabbitMQ).
 * <p>
 * Construction normalisation:
 * <ul>
 *   <li>{@code eventType} defaults to the payload's fully-qualified class name.</li>
 *   <li>{@code ctxSnapshot} defaults to an empty snapshot to avoid NPE.</li>
 *   <li>{@code headers} is normalised to an immutable empty map when null.</li>
 *   <li>{@code partitionKey} is reserved for Kafka / RabbitMQ partitioning.</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
public record AsyncEventEnvelope<T extends AsyncEvent>(
        String eventId,
        String eventType,
        long occurredAtMillis,
        long deliverAtMillis,
        EventCtxSnapshot ctxSnapshot,
        T payload,
        String source,
        String producerService,
        String partitionKey,
        Map<String, String> headers
) {

    /**
     * 归一化并校验事件信封核心字段，确保后续传输与消费链路可以按统一约束处理。
     */
    public AsyncEventEnvelope {
        Objects.requireNonNull(eventId, "eventId required");
        Objects.requireNonNull(payload, "payload required");
        if (eventType == null || eventType.isBlank()) {
            eventType = payload.getClass().getName();
        }
        if (deliverAtMillis <= 0L) {
            deliverAtMillis = occurredAtMillis;
        }
        if (ctxSnapshot == null) {
            ctxSnapshot = EventCtxSnapshot.empty();
        }
        if (headers == null || headers.isEmpty()) {
            headers = Map.of();
        } else {
            headers = Map.copyOf(headers);
        }
    }
}

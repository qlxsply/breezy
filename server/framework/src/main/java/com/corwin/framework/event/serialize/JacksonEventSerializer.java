package com.corwin.framework.event.serialize;

import com.corwin.framework.event.model.AsyncEvent;
import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.model.EventCtxSnapshot;
import com.corwin.framework.json.Json;

import java.util.Map;
import java.util.Objects;

/**
 * Jackson-based {@link EventSerializer} implementation.
 * <p>
 * Uses a two-phase deserialisation strategy: first reads the envelope shell
 * and the generic payload structure, then resolves the concrete event class
 * via {@code eventType} and restores the typed payload. This preserves type
 * information without requiring manual per-type binding configuration.
 *
 * @author Corwin 2026/4/10
 */
public class JacksonEventSerializer implements EventSerializer {

    /**
     * 将事件信封编码为 JSON 字节数组。
     */
    @Override
    public byte[] serialize(AsyncEventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope required");
        SerializedEnvelope serialized = new SerializedEnvelope(envelope.eventId(), envelope.eventType(),
                envelope.occurredAtMillis(), envelope.deliverAtMillis(), envelope.ctxSnapshot(), envelope.payload(),
                envelope.source(), envelope.producerService(), envelope.partitionKey(), envelope.headers());
        try {
            return Json.toBytes(serialized);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Serialize async event failed. eventType=" + envelope.eventType(), ex);
        }
    }

    /**
     * 将 JSON 字节数组还原为事件信封，并依据 eventType 反序列化 payload。
     */
    @Override
    public AsyncEventEnvelope<?> deserialize(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes required");
        SerializedEnvelope serialized = Json.parse(bytes, SerializedEnvelope.class);
        Class<?> payloadClass = resolvePayloadClass(serialized.eventType());
        if (!AsyncEvent.class.isAssignableFrom(payloadClass)) {
            throw new IllegalStateException("Event payload class is not AsyncEvent: " + payloadClass.getName());
        }
        Object payload;
        try {
            payload = Json.convert(serialized.payload(), payloadClass);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Deserialize async event payload failed. eventType=" + serialized.eventType(), ex);
        }
        long deliverAtMillis = serialized.deliverAtMillis();
        if (deliverAtMillis <= 0L) {
            deliverAtMillis = serialized.occurredAtMillis();
        }
        return new AsyncEventEnvelope<>(serialized.eventId(), serialized.eventType(), serialized.occurredAtMillis(),
                deliverAtMillis, serialized.ctxSnapshot(), (AsyncEvent) payload, serialized.source(),
                serialized.producerService(), serialized.partitionKey(),
                serialized.headers() == null ? Map.of() : serialized.headers());
    }

    /**
     * 根据事件类型全限定名加载事件类。
     */
    private Class<?> resolvePayloadClass(String eventType) {
        try {
            return Class.forName(eventType);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Unable to resolve async event class: " + eventType, ex);
        }
    }

    /**
     * 序列化中间结构，用于稳定持久化/传输字段布局。
     */
    private record SerializedEnvelope(
            String eventId,
            String eventType,
            long occurredAtMillis,
            long deliverAtMillis,
            EventCtxSnapshot ctxSnapshot,
            Object payload,
            String source,
            String producerService,
            String partitionKey,
            Map<String, String> headers
    ) {
    }
}

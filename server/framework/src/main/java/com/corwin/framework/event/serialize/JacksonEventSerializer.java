package com.corwin.framework.event.serialize;

import com.corwin.framework.event.model.AsyncEvent;
import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.model.EventCtxSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 Jackson 的事件序列化实现。
 * <p>
 * 该实现采用“两段式反序列化”：
 * 先读取通用外壳字段与 payload 的 {@code JsonNode}，
 * 再依据 {@code eventType} 动态解析具体事件类并还原 payload。
 * <p>
 * 该策略既保持了事件模型的类型信息，又避免为每个事件类型手工维护绑定配置。
 *
 * @author Corwin 2026/4/10
 */
public class JacksonEventSerializer implements EventSerializer {

    private final ObjectMapper objectMapper;

    public JacksonEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper required");
    }

    /**
     * 将事件信封编码为 JSON 字节数组。
     */
    @Override
    public byte[] serialize(AsyncEventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope required");
        SerializedEnvelope serialized = new SerializedEnvelope(envelope.eventId(), envelope.eventType(),
                envelope.occurredAtMillis(), envelope.deliverAtMillis(), envelope.ctxSnapshot(),
                objectMapper.valueToTree(envelope.payload()), envelope.source(), envelope.producerService(),
                envelope.partitionKey(), envelope.headers());
        try {
            return objectMapper.writeValueAsBytes(serialized);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Serialize async event failed. eventType=" + envelope.eventType(), ex);
        }
    }

    /**
     * 将 JSON 字节数组还原为事件信封，并依据 eventType 反序列化 payload。
     */
    @Override
    public AsyncEventEnvelope<?> deserialize(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes required");
        SerializedEnvelope serialized;
        try {
            serialized = objectMapper.readValue(bytes, SerializedEnvelope.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Deserialize async event bytes failed", ex);
        }
        Class<?> payloadClass = resolvePayloadClass(serialized.eventType());
        if (!AsyncEvent.class.isAssignableFrom(payloadClass)) {
            throw new IllegalStateException("Event payload class is not AsyncEvent: " + payloadClass.getName());
        }
        Object payload;
        try {
            payload = objectMapper.treeToValue(serialized.payload(), payloadClass);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Deserialize async event payload failed. eventType=" + serialized.eventType(),
                    ex);
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
            JsonNode payload,
            String source,
            String producerService,
            String partitionKey,
            Map<String, String> headers
    ) {
    }
}


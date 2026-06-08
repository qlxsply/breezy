package com.corwin.framework.event.model;

import java.util.Map;
import java.util.Objects;

/**
 * 异步事件统一信封模型。
 * <p>
 * 该对象用于在发布与传输阶段携带完整事件信息，统一承载：
 * 事件标识、事件类型、发生时间、上下文快照、事件负载以及传输扩展元数据。
 * 无论底层是内存分发、Kafka 还是 RabbitMQ，都以该模型作为中立载体。
 * <p>
 * 该记录类型在构造时会执行标准化处理：
 * <ul>
 *     <li>当 {@code eventType} 为空时自动回退为 payload 的全限定类名。</li>
 *     <li>当 {@code ctxSnapshot} 为空时自动补全为空快照，避免消费链路空指针分支。</li>
 *     <li>当 {@code partitionKey} 供 Kafka / Rabbit 扩展使用。</li>
 *     <li>当 {@code headers} 为空时归一为不可变空 Map，非空时复制为不可变副本。in-memory 可以忽略，Kafka / RabbitMQ 可根据需要使用。</li>
 * </ul>
 *
 * @author Corwin 2026/3/31
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

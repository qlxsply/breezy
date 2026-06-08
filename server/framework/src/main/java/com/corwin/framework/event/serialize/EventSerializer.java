package com.corwin.framework.event.serialize;

import com.corwin.framework.event.model.AsyncEventEnvelope;

/**
 * 事件序列化抽象。
 * <p>
 * 用于在需要跨进程传输的 transport（如 Kafka/RabbitMQ）中，完成
 * {@link AsyncEventEnvelope} 与字节数组之间的转换。
 * 该接口只定义最小能力，不绑定具体协议格式。
 *
 * @author Corwin 2026/4/10
 */
public interface EventSerializer {

    /**
     * 将事件信封序列化为字节数组。
     *
     * @param envelope 事件信封
     * @return 可传输的字节数组
     */
    byte[] serialize(AsyncEventEnvelope<?> envelope);

    /**
     * 从字节数组反序列化出事件信封。
     *
     * @param bytes 传输字节数组
     * @return 反序列化后的事件信封
     */
    AsyncEventEnvelope<?> deserialize(byte[] bytes);
}


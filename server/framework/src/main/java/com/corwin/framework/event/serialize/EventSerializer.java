package com.corwin.framework.event.serialize;

import com.corwin.framework.event.model.AsyncEventEnvelope;

/**
 * Abstraction for event serialisation between {@link AsyncEventEnvelope} and byte arrays.
 *
 * <p>Used by cross-process transports (Kafka, RabbitMQ). Defines only the minimal contract and does
 * not prescribe a specific protocol format.
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

package com.corwin.framework.event.transport.kafka;

/**
 * Resolves an event type name to a Kafka topic name.
 *
 * @author Corwin 2026/4/9
 */
public interface EventTopicResolver {

  /**
   * 解析事件类型对应的 topic。
   *
   * @param eventType 事件类型全限定名
   * @return topic 名称
   */
  String resolveTopic(String eventType);
}

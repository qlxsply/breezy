package com.corwin.framework.event.transport.kafka;

/**
 * Kafka topic 解析器。
 * <p>
 * 负责将事件类型映射为 topic 名称，供 Kafka transport 发布与订阅绑定使用。
 *
 * @author Corwin 2026/4/10
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


package com.corwin.framework.event.group;

/**
 * 消费组解析器。
 * <p>
 * 将监听方法对应的原始组标识（可读但未必可用）转换为可落地到 MQ 的稳定组名，
 * 保证多实例部署时同一监听逻辑映射到同一消费组。
 *
 * @author Corwin 2026/4/9
 */
public interface ConsumerGroupResolver {

    /**
     * 解析最终消费组名称。
     *
     * @param rawGroup 原始组标识
     * @param applicationName 当前应用名
     * @return 归一化后的消费组名称
     */
    String resolveGroup(String rawGroup, String applicationName);
}


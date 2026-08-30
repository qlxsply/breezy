package com.corwin.framework.event.group;

/**
 * Resolves a human-readable raw consumer group identifier into a stable, MQ-compatible group name,
 * ensuring the same listener maps to the same group across all instances in a deployment.
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

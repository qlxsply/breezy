package com.corwin.framework.event.durable.store;

import com.corwin.framework.event.config.AsyncEventProperties;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Abstraction for the durable event store (the event-sourcing source of truth).
 *
 * @author Corwin 2026/4/12
 */
public interface PersistentEventStore {

  /**
   * 初始化/校验存储表结构。
   *
   * @param mode schema 初始化策略
   */
  void initializeSchema(AsyncEventProperties.Durable.SchemaInitialize mode);

  /**
   * 持久化事件与其投递记录。
   *
   * @param event 事件记录
   * @param deliveries 投递记录
   */
  void saveEvent(PersistedEvent event, List<PersistedDelivery> deliveries);

  /**
   * 加载可恢复投递。
   *
   * @param consumerGroups 本节点负责的消费组
   * @param now 当前时间
   * @param limit 最大条数
   * @return 候选投递列表
   */
  List<PersistedDelivery> loadRecoverableDeliveries(
      Set<String> consumerGroups, long now, int offset, int limit);

  /**
   * 查询事件记录。
   *
   * @param eventId 事件 ID
   * @return 事件记录
   */
  Optional<PersistedEvent> findEvent(String eventId);

  /**
   * claim 投递执行权。
   *
   * @param deliveryId 投递 ID
   * @param expectedVersion 期望版本
   * @param ownerNode 节点标识
   * @param claimUntil claim 过期时间
   * @param now 当前时间
   * @return 是否 claim 成功
   */
  boolean claimDelivery(
      String deliveryId, long expectedVersion, String ownerNode, long claimUntil, long now);

  /**
   * 标记投递成功并推进 event 完成计数。
   *
   * @param deliveryId 投递 ID
   * @param eventId 事件 ID
   * @param expectedVersion 期望版本
   * @param completedAt 完成时间
   * @param updatedAt 更新时间
   * @return 是否更新成功
   */
  boolean markDeliverySucceeded(
      String deliveryId, String eventId, long expectedVersion, long completedAt, long updatedAt);

  /**
   * 标记投递失败，恢复为 PENDING。
   *
   * @param deliveryId 投递 ID
   * @param expectedVersion 期望版本
   * @param errorMessage 异常信息
   * @param updatedAt 更新时间
   * @return 是否更新成功
   */
  boolean markDeliveryFailed(
      String deliveryId, long expectedVersion, String errorMessage, long updatedAt);

  /**
   * 标记投递取消。
   *
   * @param deliveryId 投递 ID
   * @param eventId 事件 ID
   * @param expectedVersion 期望版本
   * @param completedAt 完成时间
   * @param updatedAt 更新时间
   * @return 是否更新成功
   */
  boolean markDeliveryCancelled(
      String deliveryId, String eventId, long expectedVersion, long completedAt, long updatedAt);

  /**
   * 清理完成历史数据。
   *
   * @param deliveryCompletedBefore 删除 delivery 的完成时间上界
   * @param eventCompletedBefore 删除 event 的完成时间上界
   * @param batchSize 单批删除上限
   * @return 清理统计
   */
  CleanupResult cleanupCompleted(
      long deliveryCompletedBefore, long eventCompletedBefore, int batchSize);
}

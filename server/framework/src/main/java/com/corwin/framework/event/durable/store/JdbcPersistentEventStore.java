package com.corwin.framework.event.durable.store;

import com.corwin.framework.event.config.AsyncEventProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC-based implementation of {@link PersistentEventStore}.
 *
 * @author Corwin 2026/4/12
 */
public class JdbcPersistentEventStore implements PersistentEventStore {

  private final JdbcTemplate jdbcTemplate;
  private final JdbcDialect dialect;
  private final String eventTable;
  private final String deliveryTable;

  public JdbcPersistentEventStore(
      JdbcTemplate jdbcTemplate, JdbcDialect dialect, String tablePrefix) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate required");
    this.dialect = Objects.requireNonNull(dialect, "dialect required");
    String prefix = normalizePrefix(tablePrefix);
    this.eventTable = prefix + "async_event";
    this.deliveryTable = prefix + "async_event_delivery";
  }

  /** 初始化或校验 durable 表结构。 */
  @Override
  public void initializeSchema(AsyncEventProperties.Durable.SchemaInitialize mode) {
    Objects.requireNonNull(mode, "schema initialize mode required");
    switch (mode) {
      case NEVER -> {
        return;
      }
      case VALIDATE -> validateTables();
      case CREATE_IF_NOT_EXISTS -> createTablesIfNecessary();
      default -> throw new IllegalStateException("Unsupported schema initialize mode: " + mode);
    }
  }

  /** 持久化事件与投递记录。 */
  @Override
  public void saveEvent(PersistedEvent event, List<PersistedDelivery> deliveries) {
    Objects.requireNonNull(event, "event required");
    Objects.requireNonNull(deliveries, "deliveries required");
    jdbcTemplate.update(
        dialect.insertEventSql(eventTable),
        event.id(),
        event.eventType(),
        event.source(),
        event.payloadCodec(),
        event.payloadType(),
        event.payloadBody(),
        null,
        event.occurredAt(),
        event.deliverAt(),
        event.status().name(),
        event.subscriberCount(),
        event.completedCount(),
        event.createdAt(),
        event.updatedAt(),
        event.completedAt(),
        null,
        event.version());
    if (deliveries.isEmpty()) {
      return;
    }
    String insertDeliverySql = dialect.insertDeliverySql(deliveryTable);
    for (PersistedDelivery delivery : deliveries) {
      jdbcTemplate.update(
          insertDeliverySql,
          delivery.id(),
          delivery.eventId(),
          delivery.subscriberId(),
          delivery.consumerGroup(),
          delivery.deliverAt(),
          delivery.status().name(),
          delivery.ownerNode(),
          delivery.claimUntil(),
          delivery.attemptCount(),
          delivery.lastError(),
          delivery.createdAt(),
          delivery.updatedAt(),
          delivery.completedAt(),
          delivery.version());
    }
  }

  /** 加载当前节点消费组可恢复的 delivery。 */
  @Override
  public List<PersistedDelivery> loadRecoverableDeliveries(
      Set<String> consumerGroups, long now, int offset, int limit) {
    if (consumerGroups == null || consumerGroups.isEmpty()) {
      return List.of();
    }
    int normalizedOffset = Math.max(0, offset);
    int normalizedLimit = Math.max(1, limit);
    String placeholders = String.join(",", Collections.nCopies(consumerGroups.size(), "?"));
    String sql =
        String.format(
            Locale.ROOT, dialect.selectRecoverableDeliveriesSql(deliveryTable), placeholders);
    List<Object> parameters = new ArrayList<>(consumerGroups.size() + 3);
    parameters.addAll(consumerGroups);
    parameters.add(now);
    parameters.add(normalizedOffset);
    parameters.add(normalizedLimit);
    return jdbcTemplate.query(sql, this::mapDelivery, parameters.toArray());
  }

  /** 通过 eventId 查询事件体。 */
  @Override
  public Optional<PersistedEvent> findEvent(String eventId) {
    Objects.requireNonNull(eventId, "eventId required");
    List<PersistedEvent> rows =
        jdbcTemplate.query(dialect.selectEventByIdSql(eventTable), this::mapEvent, eventId);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(rows.get(0));
  }

  /** claim 投递执行权。 */
  @Override
  public boolean claimDelivery(
      String deliveryId, long expectedVersion, String ownerNode, long claimUntil, long now) {
    Objects.requireNonNull(deliveryId, "deliveryId required");
    Objects.requireNonNull(ownerNode, "ownerNode required");
    int updated =
        jdbcTemplate.update(
            dialect.claimDeliverySql(deliveryTable),
            ownerNode,
            claimUntil,
            now,
            deliveryId,
            now,
            expectedVersion);
    return updated > 0;
  }

  /** 标记投递成功并尝试推进 event 完成状态。 */
  @Override
  public boolean markDeliverySucceeded(
      String deliveryId, String eventId, long expectedVersion, long completedAt, long updatedAt) {
    int updated =
        jdbcTemplate.update(
            dialect.markDeliverySucceededSql(deliveryTable),
            completedAt,
            updatedAt,
            deliveryId,
            expectedVersion);
    if (updated == 0) {
      return false;
    }
    jdbcTemplate.update(dialect.completeEventSql(eventTable), updatedAt, completedAt, eventId);
    return true;
  }

  /** 标记投递失败并恢复为 PENDING。 */
  @Override
  public boolean markDeliveryFailed(
      String deliveryId, long expectedVersion, String errorMessage, long updatedAt) {
    int updated =
        jdbcTemplate.update(
            dialect.markDeliveryFailedSql(deliveryTable),
            updatedAt,
            truncate(errorMessage),
            deliveryId,
            expectedVersion);
    return updated > 0;
  }

  /** 标记投递取消。 */
  @Override
  public boolean markDeliveryCancelled(
      String deliveryId, String eventId, long expectedVersion, long completedAt, long updatedAt) {
    int updated =
        jdbcTemplate.update(
            dialect.markDeliveryCancelledSql(deliveryTable),
            completedAt,
            updatedAt,
            deliveryId,
            expectedVersion);
    if (updated == 0) {
      return false;
    }
    jdbcTemplate.update(dialect.completeEventSql(eventTable), updatedAt, completedAt, eventId);
    return true;
  }

  /** 批量清理历史完成数据。 */
  @Override
  public CleanupResult cleanupCompleted(
      long deliveryCompletedBefore, long eventCompletedBefore, int batchSize) {
    int normalizedBatchSize = Math.max(1, batchSize);
    int deletedDeliveries =
        jdbcTemplate.update(
            dialect.cleanupDeliverySql(deliveryTable),
            deliveryCompletedBefore,
            normalizedBatchSize);
    int deletedEvents =
        jdbcTemplate.update(
            dialect.cleanupEventSql(eventTable), eventCompletedBefore, normalizedBatchSize);
    return new CleanupResult(deletedDeliveries, deletedEvents);
  }

  /** 创建表（若不存在）。 */
  private void createTablesIfNecessary() {
    List<String> ddlList = dialect.createTableSql(eventTable, deliveryTable);
    for (String ddl : ddlList) {
      jdbcTemplate.execute(ddl);
    }
  }

  /** 校验 required 表存在。 */
  private void validateTables() {
    if (!tableExists(eventTable)) {
      throw new IllegalStateException("Async-event durable table missing: " + eventTable);
    }
    if (!tableExists(deliveryTable)) {
      throw new IllegalStateException("Async-event durable table missing: " + deliveryTable);
    }
  }

  /** 检查指定表是否存在。 */
  private boolean tableExists(String tableName) {
    Integer count = jdbcTemplate.queryForObject(dialect.tableExistsSql(), Integer.class, tableName);
    return count != null && count > 0;
  }

  /** map event 结果集。 */
  private PersistedEvent mapEvent(ResultSet resultSet, int rowNum) throws SQLException {
    return new PersistedEvent(
        resultSet.getString("id"),
        resultSet.getString("event_type"),
        resultSet.getString("source"),
        resultSet.getString("payload_codec"),
        resultSet.getString("payload_type"),
        resultSet.getString("payload_body"),
        resultSet.getLong("occurred_at"),
        resultSet.getLong("deliver_at"),
        EventStatus.valueOf(resultSet.getString("status")),
        resultSet.getInt("subscriber_count"),
        resultSet.getInt("completed_count"),
        resultSet.getLong("created_at"),
        resultSet.getLong("updated_at"),
        nullableLong(resultSet, "completed_at"),
        resultSet.getLong("version"));
  }

  /** map delivery 结果集。 */
  private PersistedDelivery mapDelivery(ResultSet resultSet, int rowNum) throws SQLException {
    return new PersistedDelivery(
        resultSet.getString("id"),
        resultSet.getString("event_id"),
        resultSet.getString("subscriber_id"),
        resultSet.getString("consumer_group"),
        resultSet.getLong("deliver_at"),
        DeliveryStatus.valueOf(resultSet.getString("status")),
        resultSet.getString("owner_node"),
        nullableLong(resultSet, "claim_until"),
        resultSet.getInt("attempt_count"),
        resultSet.getString("last_error"),
        resultSet.getLong("created_at"),
        resultSet.getLong("updated_at"),
        nullableLong(resultSet, "completed_at"),
        resultSet.getLong("version"));
  }

  /** 读取可空 long。 */
  private Long nullableLong(ResultSet resultSet, String columnName) throws SQLException {
    long value = resultSet.getLong(columnName);
    return resultSet.wasNull() ? null : value;
  }

  /** 归一化表名前缀。 */
  private String normalizePrefix(String tablePrefix) {
    if (tablePrefix == null || tablePrefix.isBlank()) {
      return "";
    }
    return tablePrefix.trim();
  }

  /** 截断错误信息，避免超字段长度。 */
  private String truncate(String message) {
    if (message == null) {
      return null;
    }
    if (message.length() <= 2000) {
      return message;
    }
    return message.substring(0, 2000);
  }
}

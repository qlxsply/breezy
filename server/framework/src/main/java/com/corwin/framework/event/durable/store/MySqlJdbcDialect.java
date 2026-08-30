package com.corwin.framework.event.durable.store;

import java.util.List;

/**
 * {@link JdbcDialect} implementation for MySQL.
 *
 * @author Corwin 2026/4/12
 */
public class MySqlJdbcDialect implements JdbcDialect {

  @Override
  public DatabaseVendor vendor() {
    return DatabaseVendor.MYSQL;
  }

  @Override
  public List<String> createTableSql(String eventTable, String deliveryTable) {
    String createEventSql =
        "create table if not exists "
            + eventTable
            + " ("
            + "id varchar(64) not null,"
            + "event_type varchar(255) not null,"
            + "source varchar(255) null,"
            + "payload_codec varchar(64) not null,"
            + "payload_type varchar(255) not null,"
            + "payload_body longtext not null,"
            + "ctx_body longtext null,"
            + "occurred_at bigint not null,"
            + "deliver_at bigint not null,"
            + "status varchar(32) not null,"
            + "subscriber_count int not null,"
            + "completed_count int not null,"
            + "created_at bigint not null,"
            + "updated_at bigint not null,"
            + "completed_at bigint null,"
            + "deleted_at bigint null,"
            + "version bigint not null,"
            + "primary key (id),"
            + "key idx_async_event_status (status),"
            + "key idx_async_event_completed_at (completed_at),"
            + "key idx_async_event_deliver_at (deliver_at)"
            + ") engine=InnoDB default charset=utf8mb4";
    String createDeliverySql =
        "create table if not exists "
            + deliveryTable
            + " ("
            + "id varchar(64) not null,"
            + "event_id varchar(64) not null,"
            + "subscriber_id varchar(255) not null,"
            + "consumer_group varchar(255) not null,"
            + "deliver_at bigint not null,"
            + "status varchar(32) not null,"
            + "owner_node varchar(255) null,"
            + "claim_until bigint null,"
            + "attempt_count int not null,"
            + "last_error varchar(2000) null,"
            + "created_at bigint not null,"
            + "updated_at bigint not null,"
            + "completed_at bigint null,"
            + "version bigint not null,"
            + "primary key (id),"
            + "key idx_async_event_delivery_event_id (event_id),"
            + "key idx_async_event_delivery_group_status_time (consumer_group, status, deliver_at),"
            + "key idx_async_event_delivery_status_time (status, deliver_at),"
            + "key idx_async_event_delivery_claim_until (claim_until)"
            + ") engine=InnoDB default charset=utf8mb4";
    return List.of(createEventSql, createDeliverySql);
  }

  @Override
  public String tableExistsSql() {
    return "select count(1) from information_schema.tables where table_schema = database() and table_name = ?";
  }

  @Override
  public String insertEventSql(String eventTable) {
    return "insert into "
        + eventTable
        + " ("
        + "id, event_type, source, payload_codec, payload_type, payload_body, ctx_body, occurred_at, deliver_at, "
        + "status, subscriber_count, completed_count, created_at, updated_at, completed_at, deleted_at, version"
        + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  public String insertDeliverySql(String deliveryTable) {
    return "insert into "
        + deliveryTable
        + " ("
        + "id, event_id, subscriber_id, consumer_group, deliver_at, status, owner_node, claim_until, attempt_count, "
        + "last_error, created_at, updated_at, completed_at, version"
        + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  @Override
  public String selectRecoverableDeliveriesSql(String deliveryTable) {
    return "select id, event_id, subscriber_id, consumer_group, deliver_at, status, owner_node, claim_until, "
        + "attempt_count, last_error, created_at, updated_at, completed_at, version"
        + " from "
        + deliveryTable
        + " where consumer_group in (%s)"
        + " and (status = 'PENDING' or (status = 'CLAIMED' and claim_until < ?))"
        + " order by deliver_at asc, created_at asc, id asc limit ?, ?";
  }

  @Override
  public String selectEventByIdSql(String eventTable) {
    return "select id, event_type, source, payload_codec, payload_type, payload_body, occurred_at, deliver_at, status, "
        + "subscriber_count, completed_count, created_at, updated_at, completed_at, version"
        + " from "
        + eventTable
        + " where id = ?";
  }

  @Override
  public String claimDeliverySql(String deliveryTable) {
    return "update "
        + deliveryTable
        + " set "
        + "status = 'CLAIMED', owner_node = ?, claim_until = ?, attempt_count = attempt_count + 1, "
        + "updated_at = ?, version = version + 1"
        + " where id = ?"
        + " and (status = 'PENDING' or (status = 'CLAIMED' and claim_until < ?))"
        + " and version = ?";
  }

  @Override
  public String markDeliverySucceededSql(String deliveryTable) {
    return "update "
        + deliveryTable
        + " set "
        + "status = 'SUCCEEDED', completed_at = ?, updated_at = ?, owner_node = null, claim_until = null, "
        + "last_error = null, version = version + 1"
        + " where id = ? and status = 'CLAIMED' and version = ?";
  }

  @Override
  public String markDeliveryFailedSql(String deliveryTable) {
    return "update "
        + deliveryTable
        + " set "
        + "status = 'PENDING', updated_at = ?, owner_node = null, claim_until = null, "
        + "last_error = ?, version = version + 1"
        + " where id = ? and status = 'CLAIMED' and version = ?";
  }

  @Override
  public String markDeliveryCancelledSql(String deliveryTable) {
    return "update "
        + deliveryTable
        + " set "
        + "status = 'CANCELLED', completed_at = ?, updated_at = ?, owner_node = null, claim_until = null, "
        + "version = version + 1"
        + " where id = ? and status in ('PENDING', 'CLAIMED') and version = ?";
  }

  @Override
  public String completeEventSql(String eventTable) {
    return "update "
        + eventTable
        + " set "
        + "completed_count = completed_count + 1, "
        + "updated_at = ?, "
        + "status = case when completed_count + 1 >= subscriber_count then 'COMPLETED' else status end, "
        + "completed_at = case when completed_count + 1 >= subscriber_count then ? else completed_at end, "
        + "version = version + 1"
        + " where id = ? and status = 'ACTIVE'";
  }

  @Override
  public String cleanupDeliverySql(String deliveryTable) {
    return "delete from "
        + deliveryTable
        + " where status in ('SUCCEEDED', 'CANCELLED') and completed_at is not null "
        + "and completed_at < ? limit ?";
  }

  @Override
  public String cleanupEventSql(String eventTable) {
    return "delete from "
        + eventTable
        + " where status = 'COMPLETED' and completed_at is not null and completed_at < ? "
        + "limit ?";
  }
}

package com.corwin.framework.event.durable.store;

import java.util.List;

/**
 * durable JDBC 方言抽象。
 *
 * @author Corwin 2026/4/12
 */
public interface JdbcDialect {

    /**
     * 数据库类型。
     */
    DatabaseVendor vendor();

    /**
     * 建表 SQL。
     */
    List<String> createTableSql(String eventTable, String deliveryTable);

    /**
     * 表存在性校验 SQL。
     */
    String tableExistsSql();

    /**
     * 写入事件 SQL。
     */
    String insertEventSql(String eventTable);

    /**
     * 写入投递 SQL。
     */
    String insertDeliverySql(String deliveryTable);

    /**
     * 查询恢复候选 SQL。
     */
    String selectRecoverableDeliveriesSql(String deliveryTable);

    /**
     * 查询事件 SQL。
     */
    String selectEventByIdSql(String eventTable);

    /**
     * claim SQL。
     */
    String claimDeliverySql(String deliveryTable);

    /**
     * 标记成功 SQL。
     */
    String markDeliverySucceededSql(String deliveryTable);

    /**
     * 标记失败 SQL。
     */
    String markDeliveryFailedSql(String deliveryTable);

    /**
     * 标记取消 SQL。
     */
    String markDeliveryCancelledSql(String deliveryTable);

    /**
     * 推进 event 完成计数 SQL。
     */
    String completeEventSql(String eventTable);

    /**
     * 清理 delivery SQL。
     */
    String cleanupDeliverySql(String deliveryTable);

    /**
     * 清理 event SQL。
     */
    String cleanupEventSql(String eventTable);
}

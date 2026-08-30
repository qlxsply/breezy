package com.corwin.reminder.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/**
 * @author Corwin 2026/1/12
 */
@Getter
@Entity
@Table(
    name = "reminder_outbox",
    indexes = {
      @Index(name = "idx_reminder_due_time", columnList = "due_time"),
      @Index(name = "idx_reminder_status", columnList = "outbox_status"),
      @Index(name = "idx_reminder_delivery_id", columnList = "delivery_id"),
      @Index(name = "uk_reminder_dedupe", columnList = "dedupe_key", unique = true)
    })
public class ReminderOutbox {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 去重键：建议 sourceType + sourceId + dueTime + advanceSeconds */
  @Column(name = "dedupe_key", nullable = false, length = 200)
  private String dedupeKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", nullable = false, length = 16)
  private SourceType sourceType;

  @Column(name = "source_id", nullable = false)
  private Long sourceId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** 触发时间点（真正要弹窗的时间） */
  @Column(name = "due_time", nullable = false)
  private Instant dueTime;

  /** 标题（展示给客户端） */
  @Column(name = "title", nullable = false, length = 300)
  private String title;

  /** 展示内容（可选） */
  @Column(name = "body", length = 1000)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(name = "outbox_status", nullable = false, length = 16)
  private ReminderOutboxStatus status = ReminderOutboxStatus.PENDING;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "acked_at")
  private Instant ackedAt;

  @Column(name = "delivery_id")
  private Long deliveryId;

  protected ReminderOutbox() {}

  public static ReminderOutbox pending(
      Long userId,
      String dedupeKey,
      SourceType sourceType,
      Long sourceId,
      Instant dueTime,
      String title,
      String body) {
    ReminderOutbox r = new ReminderOutbox();
    r.userId = userId;
    r.dedupeKey = dedupeKey;
    r.sourceType = sourceType;
    r.sourceId = sourceId;
    r.dueTime = dueTime;
    r.title = title;
    r.body = body;
    r.status = ReminderOutboxStatus.PENDING;
    r.createdAt = HighDate.mockInstant();
    return r;
  }

  public void bindDelivery(Long deliveryId) {
    this.deliveryId = deliveryId;
  }

  public void markSent() {
    this.status = ReminderOutboxStatus.SENT;
    this.sentAt = HighDate.mockInstant();
  }

  public void markAcked() {
    this.status = ReminderOutboxStatus.ACKED;
    this.ackedAt = HighDate.mockInstant();
  }

  public void cancel() {
    this.status = ReminderOutboxStatus.CANCELED;
  }
}

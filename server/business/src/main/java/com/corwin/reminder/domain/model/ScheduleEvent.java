package com.corwin.reminder.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * @author Corwin 2026/1/12
 */
@Getter
@Entity
@Table(
    name = "schedule_event",
    indexes = {
      @Index(name = "idx_schedule_start_time", columnList = "start_time"),
      @Index(name = "idx_schedule_status", columnList = "event_status")
    })
public class ScheduleEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "title", nullable = false, length = 300)
  private String title;

  /** 第一次发生的开始时间 */
  @Column(name = "start_time", nullable = false)
  private Instant startTime;

  @Column(name = "end_time")
  private Instant endTime;

  @Column(name = "event_tz", nullable = false, length = 64)
  private String eventTimeZoneId;

  /** 简化规则 */
  @Embedded private RecurrenceRule rule;

  /** 提醒提前秒数列表 */
  @Convert(converter = com.corwin.framework.jpa.IntegerListStringConverter.class)
  @Column(name = "advance_seconds_json", nullable = false, length = 1000)
  private List<Integer> advanceSecondsList = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "event_status", nullable = false, length = 16)
  private ScheduleEventStatus status = ScheduleEventStatus.ACTIVE;

  @Column(name = "note", length = 1000)
  private String note;

  @Column(name = "owner_id")
  private Long ownerId;

  protected ScheduleEvent() {}

  public static ScheduleEvent create(
      String title,
      Instant startTime,
      Instant endTime,
      String eventTimeZoneId,
      RecurrenceRule rule,
      List<Integer> advanceSecondsList,
      String note) {
    ScheduleEvent e = new ScheduleEvent();
    e.title = Objects.requireNonNull(title, "title required").trim();
    e.startTime = Objects.requireNonNull(startTime, "startTime required");
    e.endTime = endTime;
    e.eventTimeZoneId = Objects.requireNonNull(eventTimeZoneId, "eventTimeZoneId required").trim();
    e.rule = Objects.requireNonNull(rule, "rule required");
    e.note = note;

    if (advanceSecondsList == null || advanceSecondsList.isEmpty()) {
      e.advanceSecondsList = List.of(180);
    } else {
      e.advanceSecondsList = new ArrayList<>(advanceSecondsList);
    }
    return e;
  }

  public void update(
      String title,
      Instant startTime,
      Instant endTime,
      String eventTimeZoneId,
      RecurrenceRule rule,
      List<Integer> advanceSecondsList,
      String note) {
    this.title = Objects.requireNonNull(title, "title required").trim();
    this.startTime = Objects.requireNonNull(startTime, "startTime required");
    this.endTime = endTime;
    this.eventTimeZoneId =
        Objects.requireNonNull(eventTimeZoneId, "eventTimeZoneId required").trim();
    this.rule = Objects.requireNonNull(rule, "rule required");
    this.note = note;

    if (advanceSecondsList == null || advanceSecondsList.isEmpty()) {
      this.advanceSecondsList = List.of(180);
    } else {
      this.advanceSecondsList = new ArrayList<>(advanceSecondsList);
    }
  }

  public void assignOwner(Long ownerId) {
    this.ownerId = ownerId;
  }

  public boolean isRecurring() {
    return rule != null && !rule.isOnce();
  }

  public boolean isActive() {
    return status == ScheduleEventStatus.ACTIVE;
  }

  public void pause() {
    status = ScheduleEventStatus.PAUSED;
  }

  public void activate() {
    status = ScheduleEventStatus.ACTIVE;
  }

  public void cancel() {
    status = ScheduleEventStatus.CANCELED;
  }
}

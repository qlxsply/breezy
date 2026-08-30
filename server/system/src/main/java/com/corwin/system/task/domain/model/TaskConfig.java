package com.corwin.system.task.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entity representing the dynamic runtime configuration of a task.
 *
 * @author Corwin 2026/3/30
 */
@Entity
@Table(name = "sys_task_config")
public class TaskConfig {

  @Id
  @Column(length = 64)
  private String code;

  @Column(name = "cron_expr", length = 64)
  private String cronExpr;

  @Enumerated(EnumType.STRING)
  @Column(name = "task_status", nullable = false, length = 32)
  private TaskStatus taskStatus = TaskStatus.UNPUBLISHED;

  @Column(name = "last_run_at")
  private Instant lastRunAt;

  @Column(name = "last_run_status", length = 32)
  private String lastRunStatus;

  @Column(name = "last_error_message", length = 2000)
  private String lastErrorMessage;

  public String getCode() {
    return code;
  }

  public String getCronExpr() {
    return cronExpr;
  }

  public TaskStatus getTaskStatus() {
    return taskStatus;
  }

  public Instant getLastRunAt() {
    return lastRunAt;
  }

  public String getLastRunStatus() {
    return lastRunStatus;
  }

  public String getLastErrorMessage() {
    return lastErrorMessage;
  }

  /**
   * Updates the cron expression for this task.
   *
   * @param cronExpr the new cron expression
   */
  public void updateCronExpr(String cronExpr) {
    this.cronExpr = cronExpr;
  }

  /** Marks this configuration as published, enabling scheduled execution. */
  public void markPublished() {
    this.taskStatus = TaskStatus.PUBLISHED;
  }

  /** Marks this configuration as disabled, stopping scheduled execution. */
  public void markDisabled() {
    this.taskStatus = TaskStatus.DISABLED;
  }

  /**
   * Factory method to create a default task configuration with UNPUBLISHED status.
   *
   * @param code the unique task code
   * @return a new TaskConfig instance
   */
  public static TaskConfig createDefault(String code) {
    TaskConfig config = new TaskConfig();
    config.code = code;
    config.taskStatus = TaskStatus.UNPUBLISHED;
    return config;
  }
}

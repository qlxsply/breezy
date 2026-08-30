package com.corwin.reminder.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

/**
 * @author Corwin 2026/1/12
 */
@Getter
@Entity
@Table(
    name = "todo_task",
    indexes = {
      @Index(name = "idx_todo_due_time", columnList = "due_time"),
      @Index(name = "idx_todo_status", columnList = "task_status"),
      @Index(name = "idx_todo_sort_no", columnList = "sort_no")
    })
public class TodoTask {

  private static final int REMIND_ADVANCE_SECONDS = 300;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 待办内容 */
  @Column(name = "content", length = 2000)
  private String content;

  /** 要做的时间点（可理解为截止/开始时间，简单起见用一个点） */
  @Column(name = "due_time")
  private Instant dueTime;

  /** 提醒时间点（固定由截止时间提前5分钟计算，截止为空则不提醒） */
  @Column(name = "remind_at")
  private Instant remindAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "task_status", nullable = false, length = 16)
  private TodoTaskStatus status = TodoTaskStatus.TODO;

  @Column(name = "sort_no", nullable = false)
  private Long sortNo = 0L;

  @Column(name = "owner_id")
  private Long ownerId;

  /** 用户自定义备注（可选） */
  @Column(name = "note", length = 2000)
  private String note;

  @Column(name = "completion_note", length = 2000)
  private String completionNote;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Transient private final List<TodoAttachment> attachments = new ArrayList<>();

  protected TodoTask() {}

  public static TodoTask create(String content, Instant dueTime) {
    TodoTask t = new TodoTask();
    t.content = normalizeContent(content);
    t.dueTime = dueTime;
    t.remindAt = t.resolveRemindAt(dueTime);
    t.status = TodoTaskStatus.TODO;
    return t;
  }

  public void update(String content, Instant dueTime, String note) {
    this.content = normalizeContent(content);
    this.dueTime = dueTime;
    this.note = note;
    this.remindAt = resolveRemindAt(dueTime);
  }

  public void assignOwner(Long ownerId) {
    this.ownerId = ownerId;
  }

  public void markDone() {
    markDone(this.completionNote);
  }

  public void markDone(String completionNote) {
    this.status = TodoTaskStatus.DONE;
    this.completedAt = HighDate.mockInstant();
    this.completionNote = completionNote;
  }

  public void pause() {
    this.status = TodoTaskStatus.PAUSED;
  }

  public void resume() {
    this.status = TodoTaskStatus.TODO;
  }

  public TodoTaskStatus getStatus() {
    if (status == null) {
      return TodoTaskStatus.TODO;
    }
    return status;
  }

  public void applySortNo(Long sortNo) {
    this.sortNo = Objects.requireNonNullElse(sortNo, 0L);
  }

  /** 领域规则：是否需要触发提醒（待办且到点） */
  public boolean shouldFire(Instant now) {
    if (now == null) {
      return false;
    }
    if (getStatus() != TodoTaskStatus.TODO) {
      return false;
    }
    if (remindAt == null) {
      return false;
    }
    return !now.isBefore(remindAt);
  }

  @PrePersist
  private void onCreate() {
    Instant now = HighDate.mockInstant();
    this.createdAt = Objects.requireNonNullElse(this.createdAt, now);
    this.updatedAt = now;
    this.status = getStatus();
    this.sortNo = Objects.requireNonNullElse(this.sortNo, 0L);
  }

  @PreUpdate
  private void onUpdate() {
    this.updatedAt = HighDate.mockInstant();
    this.status = getStatus();
  }

  private Instant resolveRemindAt(Instant dueTime) {
    if (dueTime == null) {
      return null;
    }
    return dueTime.minusSeconds(REMIND_ADVANCE_SECONDS);
  }

  public boolean isDone() {
    return getStatus() == TodoTaskStatus.DONE;
  }

  public List<String> contentAttachmentFileIds() {
    return attachmentFileIds(TodoAttachmentStage.CONTENT);
  }

  public List<String> completionAttachmentFileIds() {
    return attachmentFileIds(TodoAttachmentStage.COMPLETION);
  }

  public List<String> allAttachmentFileIds() {
    return attachments.stream().map(TodoAttachment::fileId).toList();
  }

  public List<TodoAttachment> attachments() {
    return List.copyOf(attachments);
  }

  public void loadAttachments(List<TodoAttachment> attachments) {
    this.attachments.clear();
    if (attachments != null && !attachments.isEmpty()) {
      this.attachments.addAll(attachments);
    }
  }

  public void replaceContentAttachments(List<String> fileIds) {
    replaceAttachments(TodoAttachmentStage.CONTENT, fileIds);
  }

  public void replaceCompletionAttachments(List<String> fileIds) {
    replaceAttachments(TodoAttachmentStage.COMPLETION, fileIds);
  }

  private void replaceAttachments(TodoAttachmentStage stage, List<String> fileIds) {
    List<String> normalizedFileIds = normalizeAttachmentFileIds(fileIds);
    Long todoId = Objects.requireNonNull(this.id, "todo id required");
    attachments.removeIf(item -> item.stage() == stage);
    for (int i = 0; i < normalizedFileIds.size(); i++) {
      attachments.add(TodoAttachment.create(todoId, stage, normalizedFileIds.get(i), i));
    }
  }

  private List<String> attachmentFileIds(TodoAttachmentStage stage) {
    return attachments.stream()
        .filter(item -> item.stage() == stage)
        .sorted(
            (left, right) ->
                Long.compare(
                    Objects.requireNonNullElse(left.sortNo(), 0L),
                    Objects.requireNonNullElse(right.sortNo(), 0L)))
        .map(TodoAttachment::fileId)
        .toList();
  }

  private static List<String> normalizeAttachmentFileIds(List<String> fileIds) {
    if (fileIds == null || fileIds.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> deduped = new LinkedHashSet<>();
    for (String fileId : fileIds) {
      if (fileId == null) {
        continue;
      }
      String trimmed = fileId.trim();
      if (!trimmed.isEmpty()) {
        deduped.add(trimmed);
      }
    }
    return deduped.stream().toList();
  }

  private static String normalizeContent(String content) {
    if (content == null) {
      return null;
    }
    String trimmed = content.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}

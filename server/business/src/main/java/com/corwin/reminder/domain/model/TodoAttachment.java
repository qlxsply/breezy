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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * @author Corwin 2026/3/12
 */
@Getter
@Entity
@Table(
    name = "todo_attachment",
    indexes = {
      @Index(name = "idx_todo_attachment_todo", columnList = "todo_id"),
      @Index(name = "idx_todo_attachment_stage", columnList = "attachment_stage")
    })
public class TodoAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "todo_id", nullable = false)
  private Long todoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "attachment_stage", nullable = false, length = 16)
  private TodoAttachmentStage stage;

  @Column(name = "file_id", nullable = false, length = 64)
  private String fileId;

  @Column(name = "sort_no", nullable = false)
  private Long sortNo;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected TodoAttachment() {}

  public static TodoAttachment create(
      Long todoId, TodoAttachmentStage stage, String fileId, long sortNo) {
    TodoAttachment attachment = new TodoAttachment();
    attachment.todoId = Objects.requireNonNull(todoId, "todoId required");
    attachment.stage = Objects.requireNonNull(stage, "stage required");
    attachment.fileId = Objects.requireNonNull(fileId, "fileId required");
    attachment.sortNo = sortNo;
    return attachment;
  }

  public Long todoId() {
    return todoId;
  }

  public TodoAttachmentStage stage() {
    return stage;
  }

  public String fileId() {
    return fileId;
  }

  public Long sortNo() {
    return sortNo;
  }

  @PrePersist
  private void onCreate() {
    if (createdAt == null) {
      createdAt = HighDate.mockInstant();
    }
  }
}

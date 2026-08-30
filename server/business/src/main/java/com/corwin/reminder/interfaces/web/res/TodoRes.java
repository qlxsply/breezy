package com.corwin.reminder.interfaces.web.res;

import com.corwin.reminder.domain.model.TodoTaskStatus;
import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/1/12
 */
public record TodoRes(
    Long id,
    String content,
    Instant dueTime,
    TodoTaskStatus status,
    String note,
    List<String> contentAttachmentFileIds,
    List<String> completionAttachmentFileIds,
    Long sortNo,
    String completionNote,
    Instant completedAt,
    Instant createdAt) {}

package com.corwin.reminder.application.view;

import com.corwin.reminder.domain.model.TodoTaskStatus;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public record TodoView(
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
        Instant createdAt
) {
}

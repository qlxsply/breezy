package com.corwin.reminder.application.command;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public record TodoSaveCommand(
    String content,
    Instant dueTime,
    String note,
    List<String> contentAttachmentFileIds,
    List<String> completionAttachmentFileIds) {}

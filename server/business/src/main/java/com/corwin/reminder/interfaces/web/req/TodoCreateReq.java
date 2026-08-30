package com.corwin.reminder.interfaces.web.req;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/2/4
 */
public record TodoCreateReq(
    String content,
    Instant dueTime,
    String note,
    List<String> contentAttachmentFileIds,
    List<String> completionAttachmentFileIds) {}

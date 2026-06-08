package com.corwin.reminder.interfaces.web.res;

import java.time.LocalDateTime;

/**
 * @author Corwin 2026/3/12
 */
public record TodoAttachmentMetaRes(
        String id,
        String name,
        Long size,
        String contentType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

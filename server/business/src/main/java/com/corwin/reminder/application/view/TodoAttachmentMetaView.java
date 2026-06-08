package com.corwin.reminder.application.view;

import java.time.LocalDateTime;

/**
 * @author Corwin 2026/3/12
 */
public record TodoAttachmentMetaView(
        String id,
        String name,
        Long size,
        String contentType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

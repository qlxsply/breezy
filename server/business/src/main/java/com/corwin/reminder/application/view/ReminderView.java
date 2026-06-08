package com.corwin.reminder.application.view;

import java.time.Instant;

import com.corwin.reminder.domain.model.SourceType;

/**
 *
 * @author Corwin 2026/1/12
 */
public record ReminderView(
        Long id,
        SourceType sourceType,
        Long sourceId,
        String title,
        String body,
        Instant dueTime,
        String eventTimeZoneId
) {
}

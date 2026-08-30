package com.corwin.reminder.application.view;

import com.corwin.reminder.domain.model.SourceType;
import java.time.Instant;

/**
 * @author Corwin 2026/1/12
 */
public record ReminderView(
    Long id,
    SourceType sourceType,
    Long sourceId,
    String title,
    String body,
    Instant dueTime,
    String eventTimeZoneId) {}

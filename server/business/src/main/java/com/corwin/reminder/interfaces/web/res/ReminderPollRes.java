package com.corwin.reminder.interfaces.web.res;

import com.corwin.reminder.application.view.ReminderView;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/1/28
 */
public record ReminderPollRes(
        Instant cutoffTime,
        List<ReminderView> items
) {
}

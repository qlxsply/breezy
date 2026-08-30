package com.corwin.reminder.application.view;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/3/30
 */
public record ReminderPollView(Instant cutoffTime, List<ReminderView> items) {}

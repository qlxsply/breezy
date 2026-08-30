package com.corwin.reminder.application.view;

import java.time.LocalDate;

/**
 * @author Corwin 2026/3/12
 */
public record TodoDailyStatsView(LocalDate date, int createdCount, int completedCount) {}

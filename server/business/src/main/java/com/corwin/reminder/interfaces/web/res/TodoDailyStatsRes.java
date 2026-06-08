package com.corwin.reminder.interfaces.web.res;

import java.time.LocalDate;

/**
 * @author Corwin 2026/3/12
 */
public record TodoDailyStatsRes(
        LocalDate date,
        int createdCount,
        int completedCount
) {
}

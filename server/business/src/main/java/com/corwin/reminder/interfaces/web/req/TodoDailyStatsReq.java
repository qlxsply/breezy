package com.corwin.reminder.interfaces.web.req;

import java.time.LocalDate;

/**
 * @author Corwin 2026/7/29
 */
public record TodoDailyStatsReq(
        LocalDate startDate,
        LocalDate endDate
) {
}

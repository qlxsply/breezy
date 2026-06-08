package com.corwin.reminder.application.command;

import com.corwin.reminder.domain.model.RecurrenceRuleFrequency;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Corwin 2026/1/12
 */
public record UpsertScheduleCommand(
        Long id,
        String title,
        Instant startTime,
        Instant endTime,
        String eventTimeZoneId,
        RuleCommand rule,
        List<Integer> advanceSecondsList,
        String note
) {
    public record RuleCommand(
            RecurrenceRuleFrequency frequency,
            Integer interval,
            Set<DayOfWeek> daysOfWeek,
            // WEEKLY
            Integer dayOfMonth,
            // MONTHLY/YEARLY
            Integer monthOfYear,
            // YEARLY
            Instant until,
            Integer countLimit
    ) {
    }
}

package com.corwin.reminder.interfaces.web.res;

import com.corwin.reminder.domain.model.RecurrenceRuleFrequency;
import com.corwin.reminder.domain.model.ScheduleEventStatus;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Corwin 2026/1/12
 */
public record ScheduleRes(
        Long id,
        String title,
        Instant startTime,
        Instant endTime,
        String eventTimeZoneId,
        RuleRes rule,
        List<Integer> advanceSecondsList,
        ScheduleEventStatus status,
        String note
) {
    public record RuleRes(
            RecurrenceRuleFrequency frequency,
            Integer interval,
            Set<DayOfWeek> daysOfWeek,
            Integer dayOfMonth,
            Integer monthOfYear,
            Instant until,
            Integer countLimit
    ) {
    }
}

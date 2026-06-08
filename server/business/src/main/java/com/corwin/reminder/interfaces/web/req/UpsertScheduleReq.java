package com.corwin.reminder.interfaces.web.req;

import com.corwin.reminder.domain.model.RecurrenceRuleFrequency;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * @author Corwin 2026/1/12
 */
@Getter
@Setter
public class UpsertScheduleReq {
    private Long id;
    private String title;
    private Instant startTime;
    private Instant endTime;
    private String eventTimeZoneId;
    private RuleReq rule;
    private List<Integer> advanceSecondsList;
    private String note;

    @Getter
    @Setter
    public static class RuleReq {
        private RecurrenceRuleFrequency frequency;
        private Integer interval;
        private Set<DayOfWeek> daysOfWeek;
        private Integer dayOfMonth;
        private Integer monthOfYear;
        private Instant until;
        private Integer countLimit;
    }
}

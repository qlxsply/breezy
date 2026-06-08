package com.corwin.reminder.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Corwin 2026/1/12
 */
@Getter
@Embeddable
public class RecurrenceRule {

    @Enumerated(EnumType.STRING)
    @Column(name = "freq", nullable = false, length = 16)
    private RecurrenceRuleFrequency frequency;

    /**
     * 间隔（默认1）
     */
    @Column(name = "freq_interval", nullable = false)
    private Integer interval = 1;

    /**
     * WEEKLY：每周的哪些天触发
     * 用 bitmask 持久化，避免 ElementCollection 额外表
     */
    @Column(name = "week_days_mask")
    private Integer weekDaysMask;

    /**
     * MONTHLY/YEARLY：每月/每年的几号（1..31）
     */
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    /**
     * YEARLY：月份（1..12）
     */
    @Column(name = "month_of_year")
    private Integer monthOfYear;

    /**
     * 结束条件：until（含义：发生时间 <= until 才算有效）
     */
    @Column(name = "until_time")
    private Instant until;

    /**
     * 结束条件：最多发生次数
     * 注意：要严格支持 count，需要存储“已发生次数”或 occurrence 明细，这里先预留字段。
     */
    @Column(name = "count_limit")
    private Integer countLimit;

    protected RecurrenceRule() {
    }

    // ---------- 工厂方法（便于约束） ----------

    public static RecurrenceRule once() {
        RecurrenceRule r = new RecurrenceRule();
        r.frequency = RecurrenceRuleFrequency.ONCE;
        r.interval = 1;
        return r;
    }

    public static RecurrenceRule daily(int interval, Instant until) {
        RecurrenceRule r = new RecurrenceRule();
        r.frequency = RecurrenceRuleFrequency.DAILY;
        r.interval = normalizeInterval(interval);
        r.until = until;
        return r;
    }

    public static RecurrenceRule weekly(int interval, Set<DayOfWeek> daysOfWeek, Instant until) {
        Objects.requireNonNull(daysOfWeek, "daysOfWeek required");
        if (daysOfWeek.isEmpty()) {
            throw new IllegalArgumentException("daysOfWeek must not be empty");
        }

        RecurrenceRule r = new RecurrenceRule();
        r.frequency = RecurrenceRuleFrequency.WEEKLY;
        r.interval = normalizeInterval(interval);
        r.weekDaysMask = encodeDays(daysOfWeek);
        r.until = until;
        return r;
    }

    public static RecurrenceRule monthly(int interval, int dayOfMonth, Instant until) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("dayOfMonth must be 1..31");
        }
        RecurrenceRule r = new RecurrenceRule();
        r.frequency = RecurrenceRuleFrequency.MONTHLY;
        r.interval = normalizeInterval(interval);
        r.dayOfMonth = dayOfMonth;
        r.until = until;
        return r;
    }

    public static RecurrenceRule yearly(int interval, int monthOfYear, int dayOfMonth, Instant until) {
        if (monthOfYear < 1 || monthOfYear > 12) {
            throw new IllegalArgumentException("monthOfYear must be 1..12");
        }
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("dayOfMonth must be 1..31");
        }
        RecurrenceRule r = new RecurrenceRule();
        r.frequency = RecurrenceRuleFrequency.YEARLY;
        r.interval = normalizeInterval(interval);
        r.monthOfYear = monthOfYear;
        r.dayOfMonth = dayOfMonth;
        r.until = until;
        return r;
    }

    public boolean isOnce() {
        return frequency == RecurrenceRuleFrequency.ONCE;
    }

    public Set<DayOfWeek> getDaysOfWeek() {
        if (weekDaysMask == null) {
            return Collections.emptySet();
        }
        return decodeDays(weekDaysMask);
    }

    // ---------- 编码/解码 DayOfWeek ----------

    private static int normalizeInterval(int interval) {
        return interval <= 0 ? 1 : interval;
    }

    private static int encodeDays(Set<DayOfWeek> days) {
        int mask = 0;
        for (DayOfWeek d : days) {
            int bit = 1 << (d.getValue() - 1); // MON=1 -> bit0 ... SUN=7 -> bit6
            mask |= bit;
        }
        return mask;
    }

    private static Set<DayOfWeek> decodeDays(int mask) {
        EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            int bit = 1 << (d.getValue() - 1);
            if ((mask & bit) != 0) {
                set.add(d);
            }
        }
        return set;
    }

}
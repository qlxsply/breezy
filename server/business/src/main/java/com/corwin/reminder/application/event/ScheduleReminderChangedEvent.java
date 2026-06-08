package com.corwin.reminder.application.event;

/**
 * 日程提醒变更事件
 *
 * @author Corwin 2026/3/16
 */
public record ScheduleReminderChangedEvent(
        Long scheduleId,
        boolean removed
) {
    public static ScheduleReminderChangedEvent updated(Long scheduleId) {
        return new ScheduleReminderChangedEvent(scheduleId, false);
    }

    public static ScheduleReminderChangedEvent removed(Long scheduleId) {
        return new ScheduleReminderChangedEvent(scheduleId, true);
    }
}

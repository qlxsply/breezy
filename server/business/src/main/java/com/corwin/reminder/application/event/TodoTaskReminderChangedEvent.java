package com.corwin.reminder.application.event;

import java.time.Instant;

/**
 * 待办事项提醒变更事件
 *
 * @author Corwin 2026/3/16
 */
public record TodoTaskReminderChangedEvent(
        Long todoId,
        Instant remindAt,
        boolean removed
) {
    public static TodoTaskReminderChangedEvent updated(Long todoId, Instant remindAt) {
        return new TodoTaskReminderChangedEvent(todoId, remindAt, false);
    }

    public static TodoTaskReminderChangedEvent removed(Long todoId) {
        return new TodoTaskReminderChangedEvent(todoId, null, true);
    }
}

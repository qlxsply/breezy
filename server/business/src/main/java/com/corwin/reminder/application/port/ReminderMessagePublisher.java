package com.corwin.reminder.application.port;

/**
 * 提醒消息发布端口。
 *
 * @author Corwin 2026/3/19
 */
public interface ReminderMessagePublisher {

    ReminderDispatchResult publishReminder(Long userId, String title, String content, String route, Long outboxId);
}

package com.corwin.reminder.application.service;

import com.corwin.reminder.application.port.ReminderMessagePublisher;
import com.corwin.reminder.application.port.ReminderDispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 提醒内部调用服务 (符合 DDD 风格，通过事件驱动)
 * @author Corwin 2026/3/16
 */
@Service
@RequiredArgsConstructor
public class ReminderInternalService {

    private final ReminderMessagePublisher reminderMessagePublisher;

    public ReminderDispatchResult sendReminder(Long userId, String title, String content, String route,
            Long outboxId) {
        return reminderMessagePublisher.publishReminder(userId, title, content, route, outboxId);
    }
}

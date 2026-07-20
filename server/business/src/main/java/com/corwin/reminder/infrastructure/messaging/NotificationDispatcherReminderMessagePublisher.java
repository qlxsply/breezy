package com.corwin.reminder.infrastructure.messaging;

import com.corwin.framework.constant.UserType;
import com.corwin.reminder.application.port.ReminderDispatchResult;
import com.corwin.reminder.application.port.ReminderMessagePublisher;
import com.corwin.system.notify.published.MsgType;
import com.corwin.system.notify.application.port.MessageDispatchPort;
import com.corwin.system.notify.application.result.MessageDispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/3/19
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatcherReminderMessagePublisher implements ReminderMessagePublisher {

    private static final String BIZ_TYPE_REMINDER_OUTBOX = "REMINDER_OUTBOX";

    private final MessageDispatchPort messageDispatchPort;

    @Override
    public ReminderDispatchResult publishReminder(Long userId, String title, String content, String route,
            Long outboxId) {
        MessageDispatchResult dispatchResult = messageDispatchPort.dispatch(userId, UserType.USER,
                MsgType.TODO_REMINDER, title, content, route, BIZ_TYPE_REMINDER_OUTBOX, String.valueOf(outboxId));
        return new ReminderDispatchResult(dispatchResult.deliveryId(), dispatchResult.delivered());
    }
}

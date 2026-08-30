package com.corwin.reminder.application.service;

import com.corwin.reminder.domain.model.ReminderOutboxStatus;
import com.corwin.reminder.domain.repo.ReminderOutboxRepository;
import com.corwin.system.notify.application.event.MessageDeliveredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 处理提醒业务消息投递成功事件。
 *
 * @author Corwin 2026/4/7
 */
@Component
@RequiredArgsConstructor
public class ReminderMessageDeliveredEventHandler {

  private static final String BIZ_TYPE_REMINDER_OUTBOX = "REMINDER_OUTBOX";

  private final ReminderOutboxRepository outboxRepo;

  @EventListener
  public void onMessageDelivered(MessageDeliveredEvent event) {
    if (!BIZ_TYPE_REMINDER_OUTBOX.equals(event.bizType())) {
      return;
    }
    if (event.bizId() == null || event.bizId().isBlank()) {
      return;
    }

    long outboxId;
    try {
      outboxId = Long.parseLong(event.bizId());
    } catch (NumberFormatException ex) {
      return;
    }

    outboxRepo
        .findById(outboxId)
        .ifPresent(
            outbox -> {
              boolean changed = false;
              if (outbox.getDeliveryId() == null && event.deliveryId() != null) {
                outbox.bindDelivery(event.deliveryId());
                changed = true;
              }
              if (outbox.getStatus() == ReminderOutboxStatus.PENDING) {
                outbox.markSent();
                changed = true;
              }
              if (changed) {
                outboxRepo.save(outbox);
              }
            });
  }
}

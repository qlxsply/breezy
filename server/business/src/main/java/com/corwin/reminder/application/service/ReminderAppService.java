package com.corwin.reminder.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.util.HighDate;
import com.corwin.reminder.application.view.ReminderPollView;
import com.corwin.reminder.application.view.ReminderView;
import com.corwin.reminder.domain.model.ReminderOutbox;
import com.corwin.reminder.domain.model.ReminderOutboxStatus;
import com.corwin.reminder.domain.model.ScheduleEvent;
import com.corwin.reminder.domain.model.SourceType;
import com.corwin.reminder.domain.repo.ReminderOutboxRepository;
import com.corwin.reminder.domain.repo.ScheduleEventRepository;
import com.corwin.system.notify.application.port.MessageDeliveryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 提醒应用服务。
 *
 * @author Corwin 2026/3/19
 */
@Service
@RequiredArgsConstructor
public class ReminderAppService {

    private final ReminderOutboxRepository outboxRepo;
    private final ScheduleEventRepository scheduleRepo;
    private final MessageDeliveryPort messageDeliveryPort;

    @Transactional(readOnly = true)
    public ReminderPollView poll(Long userId, Instant startTime) {
        Instant cutoffTime = HighDate.mockInstant();
        List<ReminderView> items = Stream.concat(
                        outboxRepo.findByUserIdAndStatusAndCreatedAtGreaterThanAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                                userId, ReminderOutboxStatus.PENDING, startTime, cutoffTime).stream(),
                        outboxRepo.findByUserIdAndStatusAndCreatedAtGreaterThanAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
                                userId, ReminderOutboxStatus.SENT, startTime, cutoffTime).stream())
                .sorted(Comparator.comparing(ReminderOutbox::getCreatedAt)).map(this::toReminderView).toList();
        return new ReminderPollView(cutoffTime, items);
    }

    @Transactional
    public void ackOutbox(Long userId, Long outboxId) {
        outboxRepo.findByIdAndUserId(outboxId, userId).ifPresent(outbox -> {
            if (outbox.getStatus() != ReminderOutboxStatus.ACKED && outbox.getStatus() != ReminderOutboxStatus.CANCELED) {
                outbox.markAcked();
                outboxRepo.save(outbox);
            }
            if (outbox.getDeliveryId() != null) {
                messageDeliveryPort.ackDelivery(userId, UserType.USER, outbox.getDeliveryId());
            }
        });
    }

    @Transactional
    public void ackDelivery(Long userId, Long deliveryId) {
        messageDeliveryPort.ackDelivery(userId, UserType.USER, deliveryId);
        outboxRepo.findByDeliveryId(deliveryId).ifPresent(outbox -> {
            if (outbox.getStatus() != ReminderOutboxStatus.ACKED && outbox.getStatus() != ReminderOutboxStatus.CANCELED) {
                outbox.markAcked();
                outboxRepo.save(outbox);
            }
        });
    }

    private ReminderView toReminderView(ReminderOutbox outbox) {
        String eventTimeZoneId = null;
        if (outbox.getSourceType() == SourceType.SCHEDULE) {
            eventTimeZoneId = scheduleRepo.findById(outbox.getSourceId()).map(ScheduleEvent::getEventTimeZoneId)
                    .orElse(null);
        }
        return new ReminderView(outbox.getId(), outbox.getSourceType(), outbox.getSourceId(), outbox.getTitle(),
                outbox.getBody(), outbox.getDueTime(), eventTimeZoneId);
    }
}

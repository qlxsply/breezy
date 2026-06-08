package com.corwin.reminder.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.reminder.domain.model.ReminderOutbox;
import com.corwin.reminder.domain.model.ReminderOutboxStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Corwin 2026/1/12
 */
public interface ReminderOutboxRepository extends DomainRepository<ReminderOutbox, Long> {

    Optional<ReminderOutbox> findByDedupeKey(String dedupeKey);

    List<ReminderOutbox> findByUserIdAndStatusAndCreatedAtGreaterThanAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            Long userId, ReminderOutboxStatus status, Instant startAt, Instant endAt);

    Optional<ReminderOutbox> findByDeliveryId(Long deliveryId);

    Optional<ReminderOutbox> findByIdAndUserId(Long id, Long userId);

}

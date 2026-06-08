package com.corwin.reminder.infrastructure.persistence;

import com.corwin.reminder.domain.model.ReminderOutbox;
import com.corwin.reminder.domain.model.ReminderOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface ReminderOutboxJpaRepository extends JpaRepository<ReminderOutbox, Long> {

    Optional<ReminderOutbox> findByDedupeKey(String dedupeKey);

    List<ReminderOutbox> findByUserIdAndStatusAndCreatedAtGreaterThanAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            Long userId, ReminderOutboxStatus status, Instant startAt, Instant endAt);

    Optional<ReminderOutbox> findByDeliveryId(Long deliveryId);

    Optional<ReminderOutbox> findByIdAndUserId(Long id, Long userId);
}

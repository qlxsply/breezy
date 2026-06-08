package com.corwin.reminder.infrastructure.persistence;

import com.corwin.reminder.domain.model.ReminderOutbox;
import com.corwin.reminder.domain.model.ReminderOutboxStatus;
import com.corwin.reminder.domain.repo.ReminderOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class ReminderOutboxRepositoryJpaAdapter implements ReminderOutboxRepository {

    private final ReminderOutboxJpaRepository repo;

    @Override
    public <S extends ReminderOutbox> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ReminderOutbox> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ReminderOutbox> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ReminderOutbox entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<ReminderOutbox> findByDedupeKey(String dedupeKey) {
        return repo.findByDedupeKey(dedupeKey);
    }

    @Override
    public List<ReminderOutbox> findByUserIdAndStatusAndCreatedAtGreaterThanAndCreatedAtLessThanEqualOrderByCreatedAtAsc(
            Long userId, ReminderOutboxStatus status, Instant startAt, Instant endAt) {
        return repo.findByUserIdAndStatusAndCreatedAtGreaterThanAndCreatedAtLessThanEqualOrderByCreatedAtAsc(userId,
                status, startAt, endAt);
    }

    @Override
    public Optional<ReminderOutbox> findByDeliveryId(Long deliveryId) {
        return repo.findByDeliveryId(deliveryId);
    }

    @Override
    public Optional<ReminderOutbox> findByIdAndUserId(Long id, Long userId) {
        return repo.findByIdAndUserId(id, userId);
    }
}

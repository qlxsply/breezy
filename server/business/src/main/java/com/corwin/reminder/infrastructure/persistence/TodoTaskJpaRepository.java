package com.corwin.reminder.infrastructure.persistence;

import com.corwin.reminder.domain.model.TodoTask;
import com.corwin.reminder.domain.model.TodoTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/3/30
 */
public interface TodoTaskJpaRepository extends JpaRepository<TodoTask, Long> {

    Page<TodoTask> findByStatusInAndRemindAtLessThanEqual(List<TodoTaskStatus> statuses, Instant now,
            Pageable pageable);

    List<TodoTask> findByIdIn(List<Long> ids);

    List<TodoTask> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant startInclusive, Instant endExclusive);

    List<TodoTask> findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(Instant startInclusive,
            Instant endExclusive);
}
package com.corwin.reminder.infrastructure.persistence;

import com.corwin.reminder.domain.model.ScheduleEvent;
import com.corwin.reminder.domain.model.ScheduleEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/3/30
 */
public interface ScheduleEventJpaRepository extends JpaRepository<ScheduleEvent, Long> {

    Page<ScheduleEvent> findByStatus(ScheduleEventStatus status, Pageable pageable);

    Page<ScheduleEvent> findByStatusAndTitleContainingIgnoreCase(ScheduleEventStatus status, String titleLike,
            Pageable pageable);
}

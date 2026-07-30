package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link SchedulerJobExecution} entity.
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobExecutionJpaRepository extends JpaRepository<SchedulerJobExecution, String> {

    /** Finds executions for a job, ordered by start time descending. */
    Page<SchedulerJobExecution> findByJobIdOrderByStartTimeDesc(String jobId, Pageable pageable);
}

package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobExecutionJpaRepository extends JpaRepository<SchedulerJobExecution, String> {

    Page<SchedulerJobExecution> findByJobIdOrderByStartTimeDesc(String jobId, Pageable pageable);
}

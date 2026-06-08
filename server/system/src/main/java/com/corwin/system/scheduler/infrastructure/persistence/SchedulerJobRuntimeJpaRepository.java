package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.system.scheduler.domain.model.SchedulerJobRuntime;
import com.corwin.system.scheduler.domain.model.SchedulerJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobRuntimeJpaRepository extends JpaRepository<SchedulerJobRuntime, String> {

    List<SchedulerJobRuntime> findAllByStatus(SchedulerJobStatus status);
}

package com.corwin.system.scheduler.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.scheduler.domain.model.SchedulerJobRuntime;
import com.corwin.system.scheduler.domain.model.SchedulerJobStatus;

import java.util.List;

/**
 * Repository interface for {@link SchedulerJobRuntime} domain aggregate.
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobRuntimeRepository extends DomainRepository<SchedulerJobRuntime, String> {

    /** Returns all runtime records with the given status. */
    List<SchedulerJobRuntime> findAllByStatus(SchedulerJobStatus status);
}

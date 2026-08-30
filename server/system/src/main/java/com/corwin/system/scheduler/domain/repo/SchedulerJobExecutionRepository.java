package com.corwin.system.scheduler.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;

/**
 * Repository interface for {@link SchedulerJobExecution} domain aggregate.
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobExecutionRepository
    extends DomainRepository<SchedulerJobExecution, String> {

  /** Paginated query for execution records of a specific job. */
  PageData<SchedulerJobExecution> pageByJobId(String jobId, PageSpec spec);
}

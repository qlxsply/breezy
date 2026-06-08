package com.corwin.system.scheduler.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.scheduler.domain.model.SchedulerJobRuntime;
import com.corwin.system.scheduler.domain.model.SchedulerJobStatus;

import java.util.List;

/**
 * 任务运行态仓储。
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobRuntimeRepository extends DomainRepository<SchedulerJobRuntime, String> {

    List<SchedulerJobRuntime> findAllByStatus(SchedulerJobStatus status);
}

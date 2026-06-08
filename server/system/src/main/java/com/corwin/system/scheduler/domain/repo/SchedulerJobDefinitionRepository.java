package com.corwin.system.scheduler.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.scheduler.domain.model.SchedulerJobDefinition;
import com.corwin.system.scheduler.domain.model.SchedulerJobSource;

import java.util.List;

/**
 * 任务定义仓储。
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobDefinitionRepository extends DomainRepository<SchedulerJobDefinition, String> {

    List<SchedulerJobDefinition> findAllByDeletedFalse();

    List<SchedulerJobDefinition> findAllByEnabledTrueAndDeletedFalse();

    List<SchedulerJobDefinition> findAllBySourceAndDeletedFalse(SchedulerJobSource source);
}

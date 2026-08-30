package com.corwin.system.scheduler.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.scheduler.domain.model.SchedulerJobDefinition;
import com.corwin.system.scheduler.domain.model.SchedulerJobSource;
import java.util.List;

/**
 * Repository interface for {@link SchedulerJobDefinition} domain aggregate.
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobDefinitionRepository
    extends DomainRepository<SchedulerJobDefinition, String> {

  /** Returns all non-deleted job definitions. */
  List<SchedulerJobDefinition> findAllByDeletedFalse();

  /** Returns all enabled and non-deleted job definitions. */
  List<SchedulerJobDefinition> findAllByEnabledTrueAndDeletedFalse();

  /** Returns all non-deleted job definitions for a given source. */
  List<SchedulerJobDefinition> findAllBySourceAndDeletedFalse(SchedulerJobSource source);
}

package com.corwin.system.task.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.task.domain.model.TaskDefinition;
import java.util.List;

/**
 * Repository interface for {@link TaskDefinition} persistence operations.
 *
 * @author Corwin 2026/3/30
 */
public interface TaskDefinitionRepository extends DomainRepository<TaskDefinition, String> {
    
    /**
     * Returns all task definitions that have not been marked as removed.
     *
     * @return the list of active task definitions
     */
    List<TaskDefinition> findAllByRemovedFalse();
}

package com.corwin.system.task.infrastructure.persistence;

import com.corwin.system.task.domain.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Spring Data JPA repository for {@link TaskDefinition} entity.
 *
 * @author Corwin 2026/3/30
 */
public interface TaskDefinitionJpaRepository extends JpaRepository<TaskDefinition, String> {
    
    /**
     * Finds all task definitions that have not been logically removed.
     *
     * @return the list of active task definitions
     */
    List<TaskDefinition> findAllByRemovedFalse();
}

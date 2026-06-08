package com.corwin.system.task.infrastructure.persistence;

import com.corwin.system.task.domain.model.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 任务定义 JPA 仓储
 * 
 * @author Corwin 2026/3/30
 */
public interface TaskDefinitionJpaRepository extends JpaRepository<TaskDefinition, String> {
    
    List<TaskDefinition> findAllByRemovedFalse();
}

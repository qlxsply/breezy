package com.corwin.system.task.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.task.domain.model.TaskDefinition;
import java.util.List;

/**
 * 任务定义仓储接口
 * 
 * @author Corwin 2026/3/30
 */
public interface TaskDefinitionRepository extends DomainRepository<TaskDefinition, String> {
    
    List<TaskDefinition> findAllByRemovedFalse();
}

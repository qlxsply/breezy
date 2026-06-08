package com.corwin.system.task.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.task.domain.model.TaskConfig;
import com.corwin.system.task.domain.model.TaskStatus;
import java.util.List;

/**
 * 任务配置仓储接口
 * 
 * @author Corwin 2026/3/30
 */
public interface TaskConfigRepository extends DomainRepository<TaskConfig, String> {
    
    List<TaskConfig> findAllByTaskStatus(TaskStatus taskStatus);
}

package com.corwin.system.task.infrastructure.persistence;

import com.corwin.system.task.domain.model.TaskConfig;
import com.corwin.system.task.domain.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 任务配置 JPA 仓储
 *
 * @author Corwin 2026/3/30
 */
public interface TaskConfigJpaRepository extends JpaRepository<TaskConfig, String> {

    List<TaskConfig> findAllByTaskStatus(TaskStatus taskStatus);
}

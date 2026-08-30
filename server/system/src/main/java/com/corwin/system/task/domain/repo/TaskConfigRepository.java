package com.corwin.system.task.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.task.domain.model.TaskConfig;
import com.corwin.system.task.domain.model.TaskStatus;
import java.util.List;

/**
 * Repository interface for {@link TaskConfig} persistence operations.
 *
 * @author Corwin 2026/3/30
 */
public interface TaskConfigRepository extends DomainRepository<TaskConfig, String> {

  /**
   * Returns all task configurations matching the given status.
   *
   * @param taskStatus the status to filter by
   * @return the list of matching task configurations
   */
  List<TaskConfig> findAllByTaskStatus(TaskStatus taskStatus);
}

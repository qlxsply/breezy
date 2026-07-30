package com.corwin.system.task.infrastructure.persistence;

import com.corwin.system.task.domain.model.TaskConfig;
import com.corwin.system.task.domain.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link TaskConfig} entity.
 *
 * @author Corwin 2026/3/30
 */
public interface TaskConfigJpaRepository extends JpaRepository<TaskConfig, String> {

    /**
     * Finds all task configurations matching the given status.
     *
     * @param taskStatus the status to filter by
     * @return the list of matching task configurations
     */
    List<TaskConfig> findAllByTaskStatus(TaskStatus taskStatus);
}

package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.system.scheduler.domain.model.SchedulerJobDefinition;
import com.corwin.system.scheduler.domain.model.SchedulerJobSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobDefinitionJpaRepository extends JpaRepository<SchedulerJobDefinition, String> {

    List<SchedulerJobDefinition> findAllByDeletedFalse();

    List<SchedulerJobDefinition> findAllByEnabledTrueAndDeletedFalse();

    List<SchedulerJobDefinition> findAllBySourceAndDeletedFalse(SchedulerJobSource source);
}

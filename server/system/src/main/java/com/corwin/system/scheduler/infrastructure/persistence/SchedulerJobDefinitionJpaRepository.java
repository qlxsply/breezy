package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.system.scheduler.domain.model.SchedulerJobDefinition;
import com.corwin.system.scheduler.domain.model.SchedulerJobSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link SchedulerJobDefinition} entity.
 *
 * @author Corwin 2026/4/15
 */
public interface SchedulerJobDefinitionJpaRepository
    extends JpaRepository<SchedulerJobDefinition, String> {

  /**
   * Finds all job definitions that are not soft-deleted.
   *
   * @return list of non-deleted job definitions
   */
  List<SchedulerJobDefinition> findAllByDeletedFalse();

  /**
   * Finds all enabled and non-deleted job definitions.
   *
   * @return list of enabled, non-deleted job definitions
   */
  List<SchedulerJobDefinition> findAllByEnabledTrueAndDeletedFalse();

  /**
   * Finds all non-deleted job definitions for the given source.
   *
   * @param source the job source
   * @return list of matching job definitions
   */
  List<SchedulerJobDefinition> findAllBySourceAndDeletedFalse(SchedulerJobSource source);
}

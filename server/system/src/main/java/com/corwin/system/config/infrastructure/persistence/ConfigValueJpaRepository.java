package com.corwin.system.config.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Corwin 2026/7/30
 */
public interface ConfigValueJpaRepository extends JpaRepository<ConfigValueEntity, String> {

  List<ConfigValueEntity> findAllByOrderByConfigKeyAsc();

  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
            update ConfigValueEntity entity
               set entity.content = :content,
                   entity.schemaVersion = :schemaVersion,
                   entity.revision = :nextRevision,
                   entity.configured = :configured,
                   entity.updatedBy = :updatedBy,
                   entity.updatedAt = :updatedAt,
                   entity.updateReason = :updateReason
             where entity.configKey = :configKey
               and entity.revision = :expectedRevision
            """)
  int updateByExpectedRevision(
      @Param("configKey") String configKey,
      @Param("content") String content,
      @Param("schemaVersion") int schemaVersion,
      @Param("nextRevision") long nextRevision,
      @Param("configured") boolean configured,
      @Param("updatedBy") Long updatedBy,
      @Param("updatedAt") Instant updatedAt,
      @Param("updateReason") String updateReason,
      @Param("expectedRevision") long expectedRevision);
}

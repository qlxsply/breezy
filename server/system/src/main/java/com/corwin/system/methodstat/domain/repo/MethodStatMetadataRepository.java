package com.corwin.system.methodstat.domain.repo;

import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMetadata;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link MethodStatMetadata} persistence.
 *
 * @author Corwin 2026/3/25
 */
public interface MethodStatMetadataRepository {

  /**
   * Save or update method metadata.
   *
   * @param metadata the metadata to persist
   * @return the persisted metadata
   */
  MethodStatMetadata save(MethodStatMetadata metadata);

  /**
   * Find metadata by its method key.
   *
   * @param key the method key
   * @return optional containing the metadata if found
   */
  Optional<MethodStatMetadata> findByKey(MethodStatKey key);

  /**
   * Return all stored metadata entries.
   *
   * @return list of all metadata
   */
  List<MethodStatMetadata> findAll();

  /** Remove all metadata entries from storage. */
  void clearAll();
}

package com.corwin.system.methodstat.domain.repo;

import com.corwin.system.methodstat.domain.model.MethodStatKey;
import com.corwin.system.methodstat.domain.model.MethodStatMetadata;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/25
 */
public interface MethodStatMetadataRepository {

    MethodStatMetadata save(MethodStatMetadata metadata);

    Optional<MethodStatMetadata> findByKey(MethodStatKey key);

    List<MethodStatMetadata> findAll();

    void clearAll();
}

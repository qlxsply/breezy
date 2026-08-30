package com.corwin.jsonfmt.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.jsonfmt.domain.model.JsonFmtRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/1
 */
public interface JsonFmtRecordRepository extends DomainRepository<JsonFmtRecord, String> {

  List<JsonFmtRecord> findByUserIdOrderByOrderNoAsc(Long userId);

  List<JsonFmtRecord> findByUserIdAndNameContainingIgnoreCaseOrderByOrderNoAsc(
      Long userId, String keyword);

  Optional<JsonFmtRecord> findByIdAndUserId(String id, Long userId);

  List<JsonFmtRecord> findByUserIdAndIdIn(Long userId, Collection<String> ids);

  long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long userId, Instant startAt, Instant endAt);
}

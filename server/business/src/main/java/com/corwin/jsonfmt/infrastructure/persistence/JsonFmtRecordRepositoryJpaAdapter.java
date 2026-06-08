package com.corwin.jsonfmt.infrastructure.persistence;

import com.corwin.jsonfmt.domain.model.JsonFmtRecord;
import com.corwin.jsonfmt.domain.repo.JsonFmtRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/2
 */
@Repository
@RequiredArgsConstructor
public class JsonFmtRecordRepositoryJpaAdapter implements JsonFmtRecordRepository {

    private final JsonFmtRecordJpaRepository repo;

    @Override
    public <S extends JsonFmtRecord> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends JsonFmtRecord> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<JsonFmtRecord> findById(String id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(JsonFmtRecord entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(String id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<JsonFmtRecord> findByUserIdOrderByOrderNoAsc(Long userId) {
        return repo.findByUserIdOrderByOrderNoAsc(userId);
    }

    @Override
    public List<JsonFmtRecord> findByUserIdAndNameContainingIgnoreCaseOrderByOrderNoAsc(Long userId, String keyword) {
        return repo.findByUserIdAndNameContainingIgnoreCaseOrderByOrderNoAsc(userId, keyword);
    }

    @Override
    public Optional<JsonFmtRecord> findByIdAndUserId(String id, Long userId) {
        return repo.findByIdAndUserId(id, userId);
    }

    @Override
    public List<JsonFmtRecord> findByUserIdAndIdIn(Long userId, Collection<String> ids) {
        return repo.findByUserIdAndIdIn(userId, ids);
    }

    @Override
    public long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Long userId, Instant startAt,
            Instant endAt) {
        return repo.countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, startAt, endAt);
    }

}

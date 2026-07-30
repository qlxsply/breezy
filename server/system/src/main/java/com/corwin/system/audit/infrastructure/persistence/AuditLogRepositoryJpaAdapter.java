package com.corwin.system.audit.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.system.audit.domain.model.AuditLog;
import com.corwin.system.audit.domain.repo.AuditLogPageQuery;
import com.corwin.system.audit.domain.repo.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing {@link AuditLogRepository} by delegating to
 * JPA for CRUD operations and MyBatis for paginated dynamic queries.
 * Normalizes query string fields with LIKE patterns before execution.
 *
 * @author Corwin 2026/4/19
 */
@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryJpaAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository repo;
    private final AuditLogMybatisMapper mybatisMapper;

    @Override
    public <S extends AuditLog> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends AuditLog> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(AuditLog entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    /**
     * Executes a paginated dynamic query via the MyBatis mapper.
     * String filter fields are normalized to LIKE patterns before execution.
     *
     * @param query the query filter parameters
     * @param spec  the pagination and sort specification
     * @return a page of matching {@link AuditLog} entities
     */
    @Override
    public PageData<AuditLog> pageByQuery(AuditLogPageQuery query, PageSpec spec) {
        return mybatisMapper.pageByQuery(normalizeQuery(query), spec);
    }

    private AuditLogPageQuery normalizeQuery(AuditLogPageQuery query) {
        if (query == null) {
            return null;
        }
        return new AuditLogPageQuery(LikePatternUtils.toContainsPattern(query.traceId()), query.operatorUserId(),
                LikePatternUtils.toContainsPattern(query.operatorUsername()), query.applicationCode(),
                LikePatternUtils.toContainsPattern(query.requestUri()),
                LikePatternUtils.toContainsPattern(query.auditResource()),
                LikePatternUtils.toContainsPattern(query.auditAction()), query.auditLevel(), query.success(),
                query.startAt(), query.endAt());
    }

}

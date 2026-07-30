package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.ApiPermission;
import com.corwin.system.resource.domain.repo.ApiPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link ApiPermissionRepository}.
 *
 * <p>Delegates all CRUD operations to the underlying {@link ApiPermissionJpaRepository}.</p>
 *
 * @author Corwin 2026/4/24
 */
@Repository
@RequiredArgsConstructor
public class ApiPermissionRepositoryJpaAdapter implements ApiPermissionRepository {

    private final ApiPermissionJpaRepository repo;

    @Override
    public <S extends ApiPermission> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ApiPermission> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ApiPermission> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ApiPermission entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public void deleteByApiId(Long apiId) {
        repo.deleteByApiId(apiId);
        repo.flush();
    }
}

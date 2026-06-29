package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.ResourcePermission;
import com.corwin.system.resource.domain.repo.ResourcePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/6/29
 */
@Repository
@RequiredArgsConstructor
public class ResourcePermissionRepositoryJpaAdapter implements ResourcePermissionRepository {

    private final ResourcePermissionJpaRepository repo;

    @Override
    public <S extends ResourcePermission> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ResourcePermission> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ResourcePermission> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ResourcePermission entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<ResourcePermission> findAll() {
        return repo.findAll();
    }

    @Override
    public List<ResourcePermission> findByResourceIdIn(Collection<Long> resourceIds) {
        return repo.findByResourceIdIn(resourceIds);
    }

    @Override
    public void deleteByResourceIdIn(Collection<Long> resourceIds) {
        repo.deleteByResourceIdIn(resourceIds);
        repo.flush();
    }
}

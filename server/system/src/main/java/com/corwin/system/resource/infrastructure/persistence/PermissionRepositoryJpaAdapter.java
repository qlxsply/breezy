package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Permission;
import com.corwin.system.resource.domain.repo.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/4/19
 */
@Repository
@RequiredArgsConstructor
public class PermissionRepositoryJpaAdapter implements PermissionRepository {

    private final PermissionJpaRepository repo;

    @Override
    public <S extends Permission> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Permission> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Permission entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<Permission> findByCode(String code) {
        return repo.findByCode(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return repo.existsByCode(code);
    }

    @Override
    public List<Permission> findAll() {
        return repo.findAll();
    }

    @Override
    public List<Permission> findAllById(Iterable<Long> ids) {
        return repo.findAllById(ids);
    }
}

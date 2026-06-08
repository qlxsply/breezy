package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.FunctionPermission;
import com.corwin.system.resource.domain.repo.FunctionPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/7
 */
@Repository
@RequiredArgsConstructor
public class FunctionPermissionRepositoryJpaAdapter implements FunctionPermissionRepository {

    private final FunctionPermissionJpaRepository repo;

    @Override
    public <S extends FunctionPermission> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends FunctionPermission> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<FunctionPermission> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(FunctionPermission entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<FunctionPermission> findAll() {
        return repo.findAll();
    }

    @Override
    public List<FunctionPermission> findByFunctionIdIn(Collection<Long> functionIds) {
        return repo.findByFunctionIdIn(functionIds);
    }
}

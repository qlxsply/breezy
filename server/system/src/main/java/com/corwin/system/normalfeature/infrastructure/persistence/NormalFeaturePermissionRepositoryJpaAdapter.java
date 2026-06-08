package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeaturePermission;
import com.corwin.system.normalfeature.domain.repo.NormalFeaturePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/5
 */
@Repository
@RequiredArgsConstructor
public class NormalFeaturePermissionRepositoryJpaAdapter implements NormalFeaturePermissionRepository {

    private final NormalFeaturePermissionJpaRepository repo;

    @Override
    public <S extends NormalFeaturePermission> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends NormalFeaturePermission> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<NormalFeaturePermission> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(NormalFeaturePermission entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<NormalFeaturePermission> findAll() {
        return repo.findAll();
    }

    @Override
    public List<NormalFeaturePermission> findByFeatureIdIn(Iterable<Long> featureIds) {
        return repo.findByFeatureIdIn(featureIds);
    }
}

package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeatureUserOverride;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureUserOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/5
 */
@Repository
@RequiredArgsConstructor
public class NormalFeatureUserOverrideRepositoryJpaAdapter implements NormalFeatureUserOverrideRepository {

    private final NormalFeatureUserOverrideJpaRepository repo;

    @Override
    public <S extends NormalFeatureUserOverride> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends NormalFeatureUserOverride> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<NormalFeatureUserOverride> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(NormalFeatureUserOverride entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<NormalFeatureUserOverride> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        repo.deleteByUserId(userId);
        repo.flush();
    }
}

package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeature;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/4/20
 */
@Repository
@RequiredArgsConstructor
public class NormalFeatureRepositoryJpaAdapter implements NormalFeatureRepository {

    private final NormalFeatureJpaRepository repo;

    @Override
    public <S extends NormalFeature> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends NormalFeature> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<NormalFeature> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(NormalFeature entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<NormalFeature> findAll() {
        return repo.findAll();
    }

    @Override
    public List<NormalFeature> findByIdIn(Iterable<Long> ids) {
        return repo.findByIdIn(ids);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
        repo.flush();
    }
}

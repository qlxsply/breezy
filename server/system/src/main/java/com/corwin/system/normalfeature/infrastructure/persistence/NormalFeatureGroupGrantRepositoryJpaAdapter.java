package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupGrant;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupGrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/21
 */
@Repository
@RequiredArgsConstructor
public class NormalFeatureGroupGrantRepositoryJpaAdapter implements NormalFeatureGroupGrantRepository {
    private final NormalFeatureGroupGrantJpaRepository repo;

    @Override public <S extends NormalFeatureGroupGrant> S save(S entity) { return repo.save(entity); }
    @Override public <S extends NormalFeatureGroupGrant> List<S> saveAll(Iterable<S> entities) { return repo.saveAll(entities); }
    @Override public Optional<NormalFeatureGroupGrant> findById(Long id) { return repo.findById(id); }
    @Override public boolean existsById(Long id) { return repo.existsById(id); }
    @Override public void delete(NormalFeatureGroupGrant entity) { repo.delete(entity); repo.flush(); }
    @Override public void deleteById(Long id) { repo.deleteById(id); repo.flush(); }
    @Override public List<NormalFeatureGroupGrant> findByGroupId(Long groupId) { return repo.findByGroupId(groupId); }
    @Override public List<NormalFeatureGroupGrant> findByGroupIdIn(Iterable<Long> groupIds) { return repo.findByGroupIdIn(groupIds); }
    @Override public void deleteByGroupId(Long groupId) { repo.deleteByGroupId(groupId); repo.flush(); }
}

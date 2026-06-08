package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.system.normalfeature.domain.model.NormalFeatureGroupMember;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/21
 */
@Repository
@RequiredArgsConstructor
public class NormalFeatureGroupMemberRepositoryJpaAdapter implements NormalFeatureGroupMemberRepository {
    private final NormalFeatureGroupMemberJpaRepository repo;

    @Override public <S extends NormalFeatureGroupMember> S save(S entity) { return repo.save(entity); }
    @Override public <S extends NormalFeatureGroupMember> List<S> saveAll(Iterable<S> entities) { return repo.saveAll(entities); }
    @Override public Optional<NormalFeatureGroupMember> findById(Long id) { return repo.findById(id); }
    @Override public boolean existsById(Long id) { return repo.existsById(id); }
    @Override public void delete(NormalFeatureGroupMember entity) { repo.delete(entity); repo.flush(); }
    @Override public void deleteById(Long id) { repo.deleteById(id); repo.flush(); }
    @Override public List<NormalFeatureGroupMember> findByUserId(Long userId) { return repo.findByUserId(userId); }
    @Override public List<NormalFeatureGroupMember> findByGroupId(Long groupId) { return repo.findByGroupId(groupId); }
    @Override public void deleteByUserId(Long userId) { repo.deleteByUserId(userId); repo.flush(); }
    @Override public void deleteByGroupId(Long groupId) { repo.deleteByGroupId(groupId); repo.flush(); }
}

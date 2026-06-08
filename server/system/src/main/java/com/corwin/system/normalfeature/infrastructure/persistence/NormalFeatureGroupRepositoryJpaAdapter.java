package com.corwin.system.normalfeature.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.system.normalfeature.domain.model.NormalFeatureGroup;
import com.corwin.system.normalfeature.domain.repo.NormalFeatureGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/21
 */
@Repository
@RequiredArgsConstructor
public class NormalFeatureGroupRepositoryJpaAdapter implements NormalFeatureGroupRepository {

    private final NormalFeatureGroupJpaRepository repo;

    @Override
    public <S extends NormalFeatureGroup> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends NormalFeatureGroup> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<NormalFeatureGroup> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(NormalFeatureGroup entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<NormalFeatureGroup> page(String keyword, Boolean enabled, PageSpec spec) {
        PageSpec resolved = spec == null ? PageSpec.of(null, null, List.of()) : spec;
        if (keyword != null && !keyword.isBlank() && enabled != null) {
            return JpaPageMapper.toPageData(repo.findByEnabledAndCodeContainingIgnoreCaseOrEnabledAndNameContainingIgnoreCase(
                    enabled, keyword.trim(), enabled, keyword.trim(), JpaPageMapper.toPageable(resolved)));
        }
        if (keyword != null && !keyword.isBlank()) {
            return JpaPageMapper.toPageData(
                    repo.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword.trim(), keyword.trim(),
                            JpaPageMapper.toPageable(resolved)));
        }
        if (enabled != null) {
            return JpaPageMapper.toPageData(repo.findByEnabled(enabled, JpaPageMapper.toPageable(resolved)));
        }
        return JpaPageMapper.toPageData(repo.findAll(JpaPageMapper.toPageable(resolved)));
    }

    @Override
    public Optional<NormalFeatureGroup> findByCode(String code) {
        return repo.findByCode(code);
    }

    @Override
    public boolean existsByCode(String code) {
        return repo.existsByCode(code);
    }

    @Override
    public List<NormalFeatureGroup> findByDefaultGroupTrueAndEnabledTrue() {
        return repo.findByDefaultGroupTrueAndEnabledTrue();
    }

    @Override
    public List<NormalFeatureGroup> findByIdIn(Iterable<Long> ids) {
        return repo.findByIdIn(ids);
    }
}

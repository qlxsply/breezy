package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.system.userfeature.domain.model.UserApplicationPackage;
import com.corwin.system.userfeature.domain.repo.UserApplicationPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link UserApplicationPackageRepository}.
 *
 * <p>Uses MyBatis for paginated queries and JPA for standard CRUD.</p>
 *
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class UserApplicationPackageRepositoryJpaAdapter implements UserApplicationPackageRepository {

    private final UserApplicationPackageJpaRepository repo;
    private final UserApplicationPackageMybatisMapper mybatisMapper;

    @Override
    public <S extends UserApplicationPackage> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends UserApplicationPackage> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<UserApplicationPackage> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(UserApplicationPackage entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<UserApplicationPackage> page(String keyword, Boolean enabled, PageSpec spec) {
        return mybatisMapper.page(LikePatternUtils.toContainsPattern(keyword), enabled, spec);
    }

    @Override
    public List<UserApplicationPackage> findAll() {
        return repo.findAll();
    }

    @Override
    public List<UserApplicationPackage> findByIdIn(Iterable<Long> ids) {
        return repo.findByIdIn(ids);
    }

    @Override
    public List<UserApplicationPackage> findByDefaultPackageTrueAndEnabledTrue() {
        return repo.findByDefaultPackageTrueAndEnabledTrue();
    }

    @Override
    public boolean existsByCode(String code) {
        return repo.existsByPackageCode(code);
    }
}

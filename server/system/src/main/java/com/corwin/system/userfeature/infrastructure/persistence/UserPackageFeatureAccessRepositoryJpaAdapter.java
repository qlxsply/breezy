package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserPackageFeatureAccess;
import com.corwin.system.userfeature.domain.repo.UserPackageFeatureAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class UserPackageFeatureAccessRepositoryJpaAdapter implements UserPackageFeatureAccessRepository {

    private final UserPackageFeatureAccessJpaRepository repo;

    @Override
    public <S extends UserPackageFeatureAccess> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends UserPackageFeatureAccess> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<UserPackageFeatureAccess> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(UserPackageFeatureAccess entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<UserPackageFeatureAccess> findByPackageId(Long packageId) {
        return repo.findByPackageId(packageId);
    }

    @Override
    public List<UserPackageFeatureAccess> findByPackageIdIn(Iterable<Long> packageIds) {
        return repo.findByPackageIdIn(packageIds);
    }

    @Override
    public void deleteByPackageId(Long packageId) {
        repo.deleteByPackageId(packageId);
        repo.flush();
    }
}

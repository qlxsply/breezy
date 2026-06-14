package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.ProductFeaturePermissionBinding;
import com.corwin.system.userfeature.domain.repo.ProductFeaturePermissionBindingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class ProductFeaturePermissionBindingRepositoryJpaAdapter implements ProductFeaturePermissionBindingRepository {

    private final ProductFeaturePermissionBindingJpaRepository repo;

    @Override
    public <S extends ProductFeaturePermissionBinding> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ProductFeaturePermissionBinding> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ProductFeaturePermissionBinding> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ProductFeaturePermissionBinding entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<ProductFeaturePermissionBinding> findAll() {
        return repo.findAll();
    }

    @Override
    public List<ProductFeaturePermissionBinding> findByFeatureIdIn(Iterable<Long> featureIds) {
        return repo.findByFeatureIdIn(featureIds);
    }
}

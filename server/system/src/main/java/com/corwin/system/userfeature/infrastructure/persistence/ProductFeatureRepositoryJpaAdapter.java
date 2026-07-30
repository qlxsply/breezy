package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.ProductFeature;
import com.corwin.system.userfeature.domain.repo.ProductFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link ProductFeatureRepository}.
 *
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class ProductFeatureRepositoryJpaAdapter implements ProductFeatureRepository {

    private final ProductFeatureJpaRepository repo;

    @Override
    public <S extends ProductFeature> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ProductFeature> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ProductFeature> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ProductFeature entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<ProductFeature> findAll() {
        return repo.findAll();
    }

    @Override
    public List<ProductFeature> findByIdIn(Iterable<Long> ids) {
        return repo.findByIdIn(ids);
    }

    @Override
    public List<ProductFeature> findByApplicationId(Long applicationId) {
        return repo.findByApplicationId(applicationId);
    }

    @Override
    public List<ProductFeature> findByApplicationIdIn(Iterable<Long> applicationIds) {
        return repo.findByApplicationIdIn(applicationIds);
    }
}

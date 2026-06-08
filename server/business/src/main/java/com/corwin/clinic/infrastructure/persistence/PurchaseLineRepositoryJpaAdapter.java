package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.PurchaseLine;
import com.corwin.clinic.domain.repo.PurchaseLineRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Corwin 2026/2/8
 */
@Repository
@RequiredArgsConstructor
public class PurchaseLineRepositoryJpaAdapter implements PurchaseLineRepository {

    private final PurchaseLineJpaRepository repo;


    @Override
    public <S extends PurchaseLine> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends PurchaseLine> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<PurchaseLine> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(PurchaseLine entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<PurchaseLine> findByPurchaseId(Long purchaseId) {
        return repo.findByPurchaseId(purchaseId);
    }

    @Override
    public void deleteByPurchaseId(Long purchaseId) {
        repo.deleteByPurchaseId(purchaseId);
        repo.flush();
    }
}

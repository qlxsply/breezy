package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.SaleLine;
import com.corwin.clinic.domain.repo.SaleLineRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Corwin 2026/2/8
 */
@Repository
@RequiredArgsConstructor
public class SaleLineRepositoryJpaAdapter implements SaleLineRepository {

    private final SaleLineJpaRepository repo;


    @Override
    public <S extends SaleLine> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends SaleLine> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<SaleLine> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(SaleLine entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<SaleLine> findBySaleId(Long saleId) {
        return repo.findBySaleId(saleId);
    }

    @Override
    public void deleteBySaleId(Long saleId) {
        repo.deleteBySaleId(saleId);
        repo.flush();
    }
}

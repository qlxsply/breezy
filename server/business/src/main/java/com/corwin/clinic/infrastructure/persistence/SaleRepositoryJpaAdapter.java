package com.corwin.clinic.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.clinic.domain.model.Sale;
import com.corwin.clinic.domain.repo.SaleRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Corwin 2026/2/8
 */
@Repository
@RequiredArgsConstructor
public class SaleRepositoryJpaAdapter implements SaleRepository {

    private final SaleJpaRepository repo;


    @Override
    public <S extends Sale> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Sale> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Sale> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Sale entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<Sale> page(String customerNameLike, LocalDate startAt, LocalDate endAt, PageSpec spec) {
        LocalDate endExclusive = (endAt == null) ? null : endAt.plusDays(1);
        var page = repo.page(customerNameLike, startAt, endExclusive, JpaPageMapper.toPageable(spec));
        return JpaPageMapper.toPageData(page);
    }
}

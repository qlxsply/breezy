package com.corwin.clinic.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.clinic.domain.model.Supplier;
import com.corwin.clinic.domain.repo.SupplierRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Corwin 2026/2/8
 */
@Repository
@RequiredArgsConstructor
public class SupplierRepositoryJpaAdapter implements SupplierRepository {

    private final SupplierJpaRepository repo;


    @Override
    public <S extends Supplier> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Supplier> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Supplier> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Supplier entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<Supplier> findByNameNorm(String nameNorm) {
        return repo.findByNameNorm(nameNorm);
    }

    @Override
    public List<Supplier> findByIdIn(List<Long> ids) {
        return repo.findAllById(ids);
    }

    @Override
    public PageData<Supplier> page(String nameLike, Boolean enabled, PageSpec spec) {
        var page = repo.page(nameLike, enabled, JpaPageMapper.toPageable(spec));
        return JpaPageMapper.toPageData(page);
    }
}

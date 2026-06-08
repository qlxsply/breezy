package com.corwin.clinic.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.clinic.domain.model.ItemCategory;
import com.corwin.clinic.domain.model.ItemSku;
import com.corwin.clinic.domain.repo.ItemSkuRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Corwin 2026/2/8
 */
@Repository
@RequiredArgsConstructor
public class ItemSkuRepositoryJpaAdapter implements ItemSkuRepository {

    private final ItemSkuJpaRepository repo;


    @Override
    public <S extends ItemSku> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ItemSku> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ItemSku> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ItemSku entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<ItemSku> findBySpuIdAndDisplayName(Long spuId, String displayName) {
        return repo.findBySpuIdAndDisplayName(spuId, displayName);
    }

    @Override
    public List<ItemSku> findBySpuId(Long spuId) {
        return repo.findBySpuId(spuId);
    }

    @Override
    public List<ItemSku> findByIdIn(List<Long> ids) {
        return repo.findAllById(ids);
    }

    @Override
    public PageData<ItemSku> page(ItemCategory category, String nameLike, Boolean enabled, PageSpec spec) {
        var page = repo.page(category, nameLike, enabled, JpaPageMapper.toPageable(spec));
        return JpaPageMapper.toPageData(page);
    }
}

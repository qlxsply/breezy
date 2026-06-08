package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.ItemLedger;
import com.corwin.clinic.domain.model.LedgerBizType;
import com.corwin.clinic.domain.repo.ItemLedgerRepository;
import com.corwin.clinic.domain.repo.SkuInventory;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/2/8
 */
@Repository
@RequiredArgsConstructor
public class ItemLedgerRepositoryJpaAdapter implements ItemLedgerRepository {

    private final ItemLedgerJpaRepository repo;

    @Override
    public <S extends ItemLedger> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ItemLedger> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ItemLedger> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ItemLedger entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<ItemLedger> page(Long skuId, LedgerBizType bizType, Long supplierId, LocalDate startAt,
            LocalDate endAt, PageSpec spec) {
        LocalDate endExclusive = (endAt == null) ? null : endAt.plusDays(1);
        var page = repo.page(skuId, bizType, supplierId, startAt, endExclusive, JpaPageMapper.toPageable(spec));
        return JpaPageMapper.toPageData(page);
    }

    @Override
    public void deleteByBizTypeAndBizId(LedgerBizType bizType, Long bizId) {
        repo.deleteByBizTypeAndBizId(bizType, bizId);
        repo.flush();
    }

    @Override
    public PageData<SkuInventory> pageInventory(Long skuId, PageSpec spec) {
        var page = repo.pageInventory(skuId, JpaPageMapper.toPageable(spec));
        return JpaPageMapper.toPageData(page);
    }
}

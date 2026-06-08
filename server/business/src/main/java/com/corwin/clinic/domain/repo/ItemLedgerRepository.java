package com.corwin.clinic.domain.repo;

import com.corwin.clinic.domain.model.ItemLedger;
import com.corwin.clinic.domain.model.LedgerBizType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;

import java.time.LocalDate;

/**
 * @author Corwin 2026/2/8
 */
public interface ItemLedgerRepository extends DomainRepository<ItemLedger, Long> {
    PageData<ItemLedger> page(Long skuId, LedgerBizType bizType, Long supplierId, LocalDate startAt, LocalDate endAt,
            PageSpec spec);

    void deleteByBizTypeAndBizId(LedgerBizType bizType, Long bizId);

    PageData<SkuInventory> pageInventory(Long skuId, PageSpec spec);
}


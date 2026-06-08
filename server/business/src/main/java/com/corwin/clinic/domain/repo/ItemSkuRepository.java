package com.corwin.clinic.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.clinic.domain.model.ItemCategory;
import com.corwin.clinic.domain.model.ItemSku;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/2/8
 */
public interface ItemSkuRepository extends DomainRepository<ItemSku, Long> {
    Optional<ItemSku> findBySpuIdAndDisplayName(Long spuId, String displayName);

    List<ItemSku> findBySpuId(Long spuId);

    List<ItemSku> findByIdIn(List<Long> ids);

    PageData<ItemSku> page(ItemCategory category, String nameLike, Boolean enabled, PageSpec spec);
}

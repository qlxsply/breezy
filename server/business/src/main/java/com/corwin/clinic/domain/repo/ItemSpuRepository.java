package com.corwin.clinic.domain.repo;

import com.corwin.clinic.domain.model.ItemCategory;
import com.corwin.clinic.domain.model.ItemSpu;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/2/8
 */
public interface ItemSpuRepository extends DomainRepository<ItemSpu, Long> {
    Optional<ItemSpu> findByCategoryAndName(ItemCategory category, String name);

    List<ItemSpu> findByIdIn(List<Long> ids);
}

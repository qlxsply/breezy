package com.corwin.system.dict.domain.repo;

import com.corwin.system.dict.domain.model.DictItem;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/15
 */
public interface DictItemRepository extends DomainRepository<DictItem, String> {

    List<DictItem> findByDictTypeIdOrderBySortNoAscItemLabelAsc(String dictTypeId);

    List<DictItem> findByDictTypeIdAndEnabledTrueOrderBySortNoAscItemLabelAsc(String dictTypeId);

    Optional<DictItem> findByDictTypeIdAndItemCode(String dictTypeId, String itemCode);

    Optional<DictItem> findByDictTypeIdAndItemValue(String dictTypeId, String itemValue);

    boolean existsByDictTypeIdAndItemCode(String dictTypeId, String itemCode);

    boolean existsByDictTypeIdAndItemValue(String dictTypeId, String itemValue);

    List<DictItem> findByParentItemId(String parentItemId);

    void deleteByDictTypeId(String dictTypeId);
}

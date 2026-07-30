package com.corwin.system.dict.domain.repo;

import com.corwin.system.dict.domain.model.DictItem;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link DictItem} domain aggregate.
 * <p>Provides domain-level data access operations for dictionary items.</p>
 *
 * @author Corwin 2026/3/15
 */
public interface DictItemRepository extends DomainRepository<DictItem, String> {

    /**
     * Finds all items of a given dictionary type, ordered by sort number and label.
     */
    List<DictItem> findByDictTypeIdOrderBySortNoAscItemLabelAsc(String dictTypeId);

    /**
     * Finds all enabled items of a given dictionary type, ordered by sort number and label.
     */
    List<DictItem> findByDictTypeIdAndEnabledTrueOrderBySortNoAscItemLabelAsc(String dictTypeId);

    /**
     * Finds an item by its dictionary type id and item code.
     */
    Optional<DictItem> findByDictTypeIdAndItemCode(String dictTypeId, String itemCode);

    /**
     * Finds an item by its dictionary type id and item value.
     */
    Optional<DictItem> findByDictTypeIdAndItemValue(String dictTypeId, String itemValue);

    /**
     * Checks whether an item with the given code exists under the specified dictionary type.
     */
    boolean existsByDictTypeIdAndItemCode(String dictTypeId, String itemCode);

    /**
     * Checks whether an item with the given value exists under the specified dictionary type.
     */
    boolean existsByDictTypeIdAndItemValue(String dictTypeId, String itemValue);

    /**
     * Finds all child items by parent item id.
     */
    List<DictItem> findByParentItemId(String parentItemId);

    /**
     * Deletes all items belonging to the specified dictionary type.
     */
    void deleteByDictTypeId(String dictTypeId);
}

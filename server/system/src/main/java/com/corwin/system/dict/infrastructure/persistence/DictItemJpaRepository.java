package com.corwin.system.dict.infrastructure.persistence;

import com.corwin.system.dict.domain.model.DictItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DictItem} entity.
 *
 * @author Corwin 2026/3/30
 */
public interface DictItemJpaRepository extends JpaRepository<DictItem, String> {

    /**
     * Finds all items of a given type, ordered by sort number and label.
     */
    List<DictItem> findByDictTypeIdOrderBySortNoAscItemLabelAsc(String dictTypeId);

    /**
     * Finds all enabled items of a given type, ordered by sort number and label.
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
     * Checks whether an item with the given code exists under the specified type.
     */
    boolean existsByDictTypeIdAndItemCode(String dictTypeId, String itemCode);

    /**
     * Checks whether an item with the given value exists under the specified type.
     */
    boolean existsByDictTypeIdAndItemValue(String dictTypeId, String itemValue);

    /**
     * Finds child items by parent item id.
     */
    List<DictItem> findByParentItemId(String parentItemId);

    /**
     * Deletes all items belonging to the specified dictionary type.
     */
    void deleteByDictTypeId(String dictTypeId);
}

package com.corwin.system.dict.infrastructure.persistence;

import com.corwin.system.dict.domain.model.DictItem;
import com.corwin.system.dict.domain.repo.DictItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/15
 */
@Repository
@RequiredArgsConstructor
public class DictItemRepositoryJpaAdapter implements DictItemRepository {

    private final DictItemJpaRepository repo;

    @Override
    public <S extends DictItem> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends DictItem> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<DictItem> findById(String id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(DictItem entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(String id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<DictItem> findByDictTypeIdOrderBySortNoAscItemLabelAsc(String dictTypeId) {
        return repo.findByDictTypeIdOrderBySortNoAscItemLabelAsc(dictTypeId);
    }

    @Override
    public List<DictItem> findByDictTypeIdAndEnabledTrueOrderBySortNoAscItemLabelAsc(String dictTypeId) {
        return repo.findByDictTypeIdAndEnabledTrueOrderBySortNoAscItemLabelAsc(dictTypeId);
    }

    @Override
    public Optional<DictItem> findByDictTypeIdAndItemCode(String dictTypeId, String itemCode) {
        return repo.findByDictTypeIdAndItemCode(dictTypeId, itemCode);
    }

    @Override
    public Optional<DictItem> findByDictTypeIdAndItemValue(String dictTypeId, String itemValue) {
        return repo.findByDictTypeIdAndItemValue(dictTypeId, itemValue);
    }

    @Override
    public boolean existsByDictTypeIdAndItemCode(String dictTypeId, String itemCode) {
        return repo.existsByDictTypeIdAndItemCode(dictTypeId, itemCode);
    }

    @Override
    public boolean existsByDictTypeIdAndItemValue(String dictTypeId, String itemValue) {
        return repo.existsByDictTypeIdAndItemValue(dictTypeId, itemValue);
    }

    @Override
    public List<DictItem> findByParentItemId(String parentItemId) {
        return repo.findByParentItemId(parentItemId);
    }

    @Override
    public void deleteByDictTypeId(String dictTypeId) {
        repo.deleteByDictTypeId(dictTypeId);
        repo.flush();
    }
}

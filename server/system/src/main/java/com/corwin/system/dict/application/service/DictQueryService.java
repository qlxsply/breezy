package com.corwin.system.dict.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.system.dict.application.view.DictItemView;
import com.corwin.system.dict.domain.model.DictItem;
import com.corwin.system.dict.domain.model.DictType;
import com.corwin.system.dict.domain.repo.DictItemRepository;
import com.corwin.system.dict.domain.repo.DictTypeRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Application service for querying dictionary items.
 *
 * <p>Provides read-only access to enabled dictionary data for internal use and external consumers.
 *
 * @author Corwin 2026/3/15
 */
@Service
@RequiredArgsConstructor
public class DictQueryService {

  private final DictTypeRepository dictTypeRepository;
  private final DictItemRepository dictItemRepository;

  /** Lists all enabled items of a dictionary type identified by its code. */
  public List<DictItemView> listEnabledItems(String code) {
    DictType dictType = getEnabledType(code);
    return dictItemRepository
        .findByDictTypeIdAndEnabledTrueOrderBySortNoAscItemLabelAsc(dictType.getId())
        .stream()
        .map(this::toItemView)
        .toList();
  }

  /** Batch-queries enabled items for multiple dictionary type codes. */
  public Map<String, List<DictItemView>> batchListEnabledItems(List<String> codes) {
    Map<String, List<DictItemView>> result = new LinkedHashMap<>();
    if (codes == null || codes.isEmpty()) {
      return result;
    }
    for (String code : codes) {
      result.put(code, listEnabledItems(code));
    }
    return result;
  }

  /** Checks whether a given item value exists and is enabled under the specified dict type. */
  public boolean existsValue(String code, String value) {
    DictType dictType = getEnabledType(code);
    return dictItemRepository
        .findByDictTypeIdAndItemValue(dictType.getId(), value)
        .filter(DictItem::isEnabled)
        .isPresent();
  }

  /** Checks whether a given item code exists and is enabled under the specified dict type. */
  public boolean existsCode(String code, String itemCode) {
    DictType dictType = getEnabledType(code);
    return dictItemRepository
        .findByDictTypeIdAndItemCode(dictType.getId(), itemCode)
        .filter(DictItem::isEnabled)
        .isPresent();
  }

  /** Asserts that the given value is a valid and enabled item under the specified dict type. */
  public void assertValidValue(String code, String value) {
    BizAssert.notBlank(value, BaseError.INVALID_PARAMETER);
    if (!existsValue(code, value.trim())) {
      throw new BizException(BaseError.INVALID_PARAMETER);
    }
  }

  /** Resolves the display label for a given item value under the specified dict type. */
  public String resolveLabel(String code, String value) {
    DictType dictType = getEnabledType(code);
    return dictItemRepository
        .findByDictTypeIdAndItemValue(dictType.getId(), value)
        .filter(DictItem::isEnabled)
        .map(DictItem::getItemLabel)
        .orElse(null);
  }

  private DictType getEnabledType(String code) {
    BizAssert.notBlank(code, BaseError.MISSING_PARAMETER);
    DictType dictType =
        dictTypeRepository
            .findByCode(code.trim())
            .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    BizAssert.state(dictType.isEnabled(), BaseError.FORBIDDEN);
    return dictType;
  }

  private DictItemView toItemView(DictItem item) {
    return new DictItemView(
        item.getId(),
        item.getDictTypeId(),
        item.getParentItemId(),
        item.getItemCode(),
        item.getItemLabel(),
        item.getItemValue(),
        item.getSortNo(),
        item.isEnabled(),
        item.isDefaultItem(),
        item.getTagColor(),
        item.getTagType(),
        item.getExtraJson(),
        item.getDescription());
  }
}

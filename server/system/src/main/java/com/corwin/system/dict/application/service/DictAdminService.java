package com.corwin.system.dict.application.service;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.system.dict.application.command.CreateDictItemCommand;
import com.corwin.system.dict.application.command.SaveDictTypeItemCommand;
import com.corwin.system.dict.application.command.CreateDictTypeCommand;
import com.corwin.system.dict.application.command.UpdateDictItemCommand;
import com.corwin.system.dict.application.command.UpdateDictTypeCommand;
import com.corwin.system.dict.application.view.DictItemView;
import com.corwin.system.dict.application.view.DictTypeView;
import com.corwin.system.dict.domain.model.*;
import com.corwin.system.dict.domain.repo.DictItemRepository;
import com.corwin.system.dict.domain.repo.DictTypeRepository;
import com.corwin.system.user.domain.model.DefaultUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for dictionary type and item administration.
 * <p>Handles CRUD operations, sorting, status toggling, and draft-based batch item synchronization.</p>
 *
 * @author Corwin 2026/3/15
 */
@Service
@RequiredArgsConstructor
public class DictAdminService {

    private static final int TYPE_CODE_MAX = 128;
    private static final int TYPE_NAME_MAX = 128;
    private static final int TYPE_DESCRIPTION_MAX = 255;
    private static final int ITEM_CODE_MAX = 128;
    private static final int ITEM_LABEL_MAX = 128;
    private static final int ITEM_VALUE_MAX = 512;
    private static final int ITEM_TAG_COLOR_MAX = 32;
    private static final int ITEM_TAG_TYPE_MAX = 32;
    private static final int ITEM_EXTRA_JSON_MAX = 4000;
    private static final int ITEM_DESCRIPTION_MAX = 255;

    private final DictTypeRepository dictTypeRepository;
    private final DictItemRepository dictItemRepository;

    /**
     * Lists all dictionary types, optionally filtered by keyword matching code or name.
     */
    public List<DictTypeView> listTypes(String keyword) {
        List<DictType> types = dictTypeRepository.findAllByOrderByNameAsc();
        if (keyword == null || keyword.isBlank()) {
            return types.stream().map(this::toTypeView).toList();
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        return types.stream().filter(item -> contains(item.getCode(), kw) || contains(item.getName(), kw))
                .map(this::toTypeView).toList();
    }

    /**
     * Paginates dictionary types with optional code and name filtering.
     */
    public PageData<DictTypeView> pageTypes(String code, String name, PageSpec spec) {
        PageData<DictType> page = dictTypeRepository.page(code, name, PageSpecSorts.apply(spec));
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), page.elements().stream().map(this::toTypeView).toList());
    }

    /**
     * Gets a dictionary type by its id.
     */
    public DictTypeView getType(String id) {
        return toTypeView(getTypeEntity(id));
    }

    /**
     * Creates a new dictionary type together with its initial items.
     */
    @Transactional
    public DictTypeView createType(CreateDictTypeCommand cmd) {
        String code = normalizeCode(cmd.code());
        BizAssert.state(!dictTypeRepository.existsByCode(code), BaseError.CONFLICT);
        String operator = operator();
        DictType entity = new DictType(newId(), code, normalizeName(cmd.name()),
                trimToNull(cmd.description(), TYPE_DESCRIPTION_MAX), trimToNull(cmd.enumClass(), TYPE_DESCRIPTION_MAX),
                defaultValueType(cmd.valueType()), defaultStructureType(cmd.structureType()), DictSourceType.CUSTOM,
                cmd.enabled(), operator);
        entity = dictTypeRepository.save(entity);
        syncTypeItems(entity, cmd.items(), operator);
        return toTypeView(entity);
    }

    /**
     * Updates an existing dictionary type along with its items.
     */
    @Transactional
    public DictTypeView updateType(String id, UpdateDictTypeCommand cmd) {
        DictType entity = getTypeEntity(id);
        BizAssert.state(entity.getSourceType() != DictSourceType.BUILTIN, BaseError.FORBIDDEN);
        String operator = operator();
        entity.update(normalizeName(cmd.name()), trimToNull(cmd.description(), TYPE_DESCRIPTION_MAX),
                trimToNull(cmd.enumClass(), TYPE_DESCRIPTION_MAX), defaultValueType(cmd.valueType()),
                defaultStructureType(cmd.structureType()), cmd.enabled(), operator);
        entity = dictTypeRepository.save(entity);
        syncTypeItems(entity, cmd.items(), operator);
        return toTypeView(entity);
    }

    /**
     * Enables or disables a dictionary type.
     */
    @Transactional
    public boolean updateTypeStatus(String id, boolean enabled) {
        DictType entity = getTypeEntity(id);
        if (!enabled) {
            entity.disable(operator());
        } else {
            entity.enable(operator());
        }
        dictTypeRepository.save(entity);
        return true;
    }

    /**
     * Deletes a dictionary type and all its items.
     */
    @Transactional
    public boolean deleteType(String id) {
        DictType entity = getTypeEntity(id);
        BizAssert.state(entity.getSourceType() != DictSourceType.BUILTIN, BaseError.FORBIDDEN);
        dictItemRepository.deleteByDictTypeId(entity.getId());
        dictTypeRepository.delete(entity);
        return true;
    }

    /**
     * Lists all items of a given dictionary type.
     */
    public List<DictItemView> listItems(String typeId) {
        DictType type = getTypeEntity(typeId);
        return dictItemRepository.findByDictTypeIdOrderBySortNoAscItemLabelAsc(type.getId()).stream()
                .map(this::toItemView).toList();
    }

    /**
     * Creates a new dictionary item under the specified type.
     */
    @Transactional
    public DictItemView createItem(String typeId, CreateDictItemCommand cmd) {
        DictType type = getTypeEntity(typeId);
        String parentItemId = trimToNull(cmd.parentItemId());
        validateParent(type, parentItemId, null);
        String itemCode = normalizeCode(cmd.itemCode());
        String itemValue = normalizeItemValue(type.getValueType(), cmd.itemValue());
        boolean enabled = normalizeItemEnabled(cmd.enabled(), cmd.defaultItem());
        BizAssert.state(!dictItemRepository.existsByDictTypeIdAndItemCode(type.getId(), itemCode), BaseError.CONFLICT);
        BizAssert.state(!dictItemRepository.existsByDictTypeIdAndItemValue(type.getId(), itemValue),
                BaseError.CONFLICT);
        DictItem item = new DictItem(newId(), type.getId(), parentItemId, itemCode, normalizeItemLabel(cmd.itemLabel()),
                itemValue, normalizeSortNo(cmd.sortNo()), enabled, cmd.defaultItem(),
                trimToNull(cmd.tagColor(), ITEM_TAG_COLOR_MAX), trimToNull(cmd.tagType(), ITEM_TAG_TYPE_MAX),
                trimToNull(cmd.extraJson(), ITEM_EXTRA_JSON_MAX), trimToNull(cmd.description(), ITEM_DESCRIPTION_MAX),
                operator());
        item = dictItemRepository.save(item);
        if (item.isDefaultItem()) {
            normalizeDefaultItem(type.getId(), item.getId(), operator());
        }
        return toItemView(item);
    }

    /**
     * Updates an existing dictionary item.
     */
    @Transactional
    public DictItemView updateItem(String itemId, UpdateDictItemCommand cmd) {
        DictItem item = getItemEntity(itemId);
        DictType type = getTypeEntity(item.getDictTypeId());
        String parentItemId = trimToNull(cmd.parentItemId());
        validateParent(type, parentItemId, item.getId());
        String nextValue = normalizeItemValue(type.getValueType(), cmd.itemValue());
        boolean enabled = normalizeItemEnabled(cmd.enabled(), cmd.defaultItem());
        DictItem existingValueItem = dictItemRepository.findByDictTypeIdAndItemValue(type.getId(), nextValue)
                .orElse(null);
        if (existingValueItem != null && !existingValueItem.getId().equals(item.getId())) {
            BizAssert.fail(BaseError.CONFLICT);
        }
        item.update(parentItemId, normalizeItemLabel(cmd.itemLabel()), nextValue, normalizeSortNo(cmd.sortNo()),
                enabled, cmd.defaultItem(), trimToNull(cmd.tagColor(), ITEM_TAG_COLOR_MAX),
                trimToNull(cmd.tagType(), ITEM_TAG_TYPE_MAX), trimToNull(cmd.extraJson(), ITEM_EXTRA_JSON_MAX),
                trimToNull(cmd.description(), ITEM_DESCRIPTION_MAX), operator());
        item = dictItemRepository.save(item);
        if (item.isDefaultItem()) {
            normalizeDefaultItem(type.getId(), item.getId(), operator());
        }
        return toItemView(item);
    }

    /**
     * Enables or disables a dictionary item.
     */
    @Transactional
    public boolean updateItemStatus(String itemId, boolean enabled) {
        DictItem item = getItemEntity(itemId);
        if (!enabled) {
            BizAssert.state(!item.isDefaultItem(), BaseError.INVALID_PARAMETER);
            item.disable(operator());
        } else {
            item.enable(operator());
        }
        dictItemRepository.save(item);
        return true;
    }

    /**
     * Deletes a dictionary item if it has no children.
     */
    @Transactional
    public boolean deleteItem(String itemId) {
        DictItem item = getItemEntity(itemId);
        BizAssert.state(dictItemRepository.findByParentItemId(item.getId()).isEmpty(), BaseError.FORBIDDEN);
        dictItemRepository.delete(item);
        return true;
    }

    /**
     * Reorders items of a dictionary type according to the given id list.
     */
    @Transactional
    public boolean sortItems(String typeId, List<String> itemIds) {
        DictType type = getTypeEntity(typeId);
        BizAssert.notEmpty(itemIds, BaseError.MISSING_PARAMETER);
        List<DictItem> items = dictItemRepository.findByDictTypeIdOrderBySortNoAscItemLabelAsc(type.getId());
        BizAssert.state(items.size() == itemIds.size(), BaseError.INVALID_PARAMETER);
        for (int i = 0; i < itemIds.size(); i++) {
            String itemId = itemIds.get(i);
            DictItem item = null;
            for (DictItem current : items) {
                if (current.getId().equals(itemId)) {
                    item = current;
                    break;
                }
            }
            if (item == null) {
                throw new BizException(BaseError.INVALID_PARAMETER);
            }
            item.updateSortNo(i + 1, operator());
            dictItemRepository.save(item);
        }
        return true;
    }

    /**
     * Synchronizes the draft-based item list of a dictionary type: creates new items, updates existing ones, and deletes removed ones.
     */
    private void syncTypeItems(DictType type, List<SaveDictTypeItemCommand> itemCommands, String operator) {
        List<SaveDictTypeItemCommand> safeCommands = itemCommands == null ? List.of() : itemCommands;
        List<DictItem> existingItems = dictItemRepository.findByDictTypeIdOrderBySortNoAscItemLabelAsc(type.getId());
        Map<String, DictItem> existingById = new HashMap<>();
        for (DictItem existing : existingItems) {
            existingById.put(existing.getId(), existing);
        }

        Map<String, DraftItemPayload> payloadByClientKey = normalizeDraftItems(type, safeCommands, existingById);
        validateDraftItemRelations(type, payloadByClientKey);

        Map<String, DictItem> entityByClientKey = new LinkedHashMap<>();
        Set<String> retainedIds = new HashSet<>();
        for (DraftItemPayload payload : payloadByClientKey.values()) {
            DictItem entity = payload.id() == null
                    ? new DictItem(newId(), type.getId(), null, payload.itemCode(), payload.itemLabel(),
                            payload.itemValue(), payload.sortNo(), payload.enabled(), payload.defaultItem(),
                            payload.tagColor(), payload.tagType(), payload.extraJson(), payload.description(), operator)
                    : existingById.get(payload.id());
            BizAssert.notNull(entity, BaseError.INVALID_PARAMETER);
            if (payload.id() != null) {
                retainedIds.add(payload.id());
            }
            entityByClientKey.put(payload.clientKey(), entity);
        }

        String defaultItemId = null;
        for (DraftItemPayload payload : payloadByClientKey.values()) {
            DictItem entity = entityByClientKey.get(payload.clientKey());
            String parentItemId = payload.parentClientKey() == null ? null
                    : resolveParentItemId(payload.parentClientKey(), entityByClientKey, entity.getId());
            entity.update(parentItemId, payload.itemLabel(), payload.itemValue(), payload.sortNo(), payload.enabled(),
                    payload.defaultItem(), payload.tagColor(), payload.tagType(), payload.extraJson(),
                    payload.description(), operator);
            if (payload.defaultItem()) {
                defaultItemId = entity.getId();
            }
        }

        dictItemRepository.saveAll(entityByClientKey.values());
        normalizeDefaultItem(type.getId(), defaultItemId, operator);

        for (DictItem existing : existingItems) {
            if (!retainedIds.contains(existing.getId())) {
                dictItemRepository.delete(existing);
            }
        }
    }

    /**
     * Ensures only the specified item is marked as default within the dictionary type.
     */
    private void normalizeDefaultItem(String dictTypeId, String keepId, String operator) {
        for (DictItem existing : dictItemRepository.findByDictTypeIdOrderBySortNoAscItemLabelAsc(dictTypeId)) {
            if (keepId != null && existing.getId().equals(keepId)) {
                continue;
            }
            if (existing.isDefaultItem()) {
                existing.clearDefault(operator);
                dictItemRepository.save(existing);
            }
        }
    }

    /**
     * Retrieves the dict type entity by id, throwing NOT_FOUND if absent.
     */
    private DictType getTypeEntity(String id) {
        return dictTypeRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    /**
     * Retrieves the dict item entity by id, throwing NOT_FOUND if absent.
     */
    private DictItem getItemEntity(String id) {
        return dictItemRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    /**
     * Validates the parent item assignment: checks structure type, existence, and circular reference.
     */
    private void validateParent(DictType type, String parentItemId, String currentItemId) {
        if (parentItemId == null || parentItemId.isBlank()) {
            return;
        }
        BizAssert.state(type.getStructureType() == DictStructureType.TREE, BaseError.INVALID_PARAMETER);
        DictItem parent = dictItemRepository.findById(parentItemId.trim())
                .orElseThrow(() -> new BizException(BaseError.INVALID_PARAMETER));
        BizAssert.state(parent.getDictTypeId().equals(type.getId()), BaseError.INVALID_PARAMETER);
        if (currentItemId == null || currentItemId.isBlank()) {
            return;
        }
        BizAssert.state(!currentItemId.equals(parent.getId()), BaseError.INVALID_PARAMETER);
        assertNoCircularParent(parent.getId(), currentItemId);
    }

    /**
     * Normalizes and validates the draft item payloads from client input.
     */
    private Map<String, DraftItemPayload> normalizeDraftItems(DictType type, List<SaveDictTypeItemCommand> items,
            Map<String, DictItem> existingById) {
        Map<String, DraftItemPayload> payloadByClientKey = new LinkedHashMap<>();
        Set<String> itemCodes = new HashSet<>();
        Set<String> itemValues = new HashSet<>();
        int defaultCount = 0;
        int index = 0;
        for (SaveDictTypeItemCommand item : items) {
            index += 1;
            String clientKey = normalizeClientKey(item.clientKey());
            BizAssert.state(!payloadByClientKey.containsKey(clientKey), BaseError.CONFLICT);
            String id = trimToNull(item.id());
            if (id != null) {
                BizAssert.state(existingById.containsKey(id), BaseError.INVALID_PARAMETER);
            }
            String itemCode = normalizeItemCode(item.itemCode());
            String itemLabel = normalizeItemLabel(item.itemLabel());
            String itemValue = normalizeItemValue(type.getValueType(), item.itemValue());
            BizAssert.state(itemCodes.add(itemCode.toLowerCase(Locale.ROOT)), BaseError.CONFLICT);
            BizAssert.state(itemValues.add(itemValue), BaseError.CONFLICT);
            boolean defaultItem = item.defaultItem();
            if (defaultItem) {
                defaultCount += 1;
            }
            boolean enabled = normalizeItemEnabled(item.enabled(), defaultItem);
            payloadByClientKey.put(clientKey,
                    new DraftItemPayload(id, clientKey, trimToNull(item.parentClientKey()), itemCode, itemLabel,
                            itemValue, normalizeSubmittedSortNo(item.sortNo(), index), enabled, defaultItem,
                            trimToNull(item.tagColor(), ITEM_TAG_COLOR_MAX), trimToNull(item.tagType(), ITEM_TAG_TYPE_MAX),
                            trimToNull(item.extraJson(), ITEM_EXTRA_JSON_MAX),
                            trimToNull(item.description(), ITEM_DESCRIPTION_MAX)));
        }
        BizAssert.state(defaultCount <= 1, BaseError.INVALID_PARAMETER);
        return payloadByClientKey;
    }

    /**
     * Validates parent-child relationships among draft items to prevent cycles and invalid references.
     */
    private void validateDraftItemRelations(DictType type, Map<String, DraftItemPayload> payloadByClientKey) {
        if (type.getStructureType() != DictStructureType.TREE) {
            for (DraftItemPayload payload : payloadByClientKey.values()) {
                BizAssert.state(payload.parentClientKey() == null, BaseError.INVALID_PARAMETER);
            }
            return;
        }
        for (DraftItemPayload payload : payloadByClientKey.values()) {
            String parentClientKey = payload.parentClientKey();
            if (parentClientKey == null) {
                continue;
            }
            BizAssert.state(payloadByClientKey.containsKey(parentClientKey), BaseError.INVALID_PARAMETER);
            String cursor = parentClientKey;
            Set<String> visited = new HashSet<>();
            while (cursor != null) {
                BizAssert.state(visited.add(cursor), BaseError.INVALID_PARAMETER);
                BizAssert.state(!payload.clientKey().equals(cursor), BaseError.INVALID_PARAMETER);
                DraftItemPayload parent = payloadByClientKey.get(cursor);
                cursor = parent == null ? null : parent.parentClientKey();
            }
        }
    }

    /**
     * Resolves the actual parent item id from the client-side parent key.
     */
    private String resolveParentItemId(String parentClientKey, Map<String, DictItem> entityByClientKey, String selfId) {
        DictItem parent = entityByClientKey.get(parentClientKey);
        BizAssert.notNull(parent, BaseError.INVALID_PARAMETER);
        BizAssert.state(!parent.getId().equals(selfId), BaseError.INVALID_PARAMETER);
        return parent.getId();
    }

    /**
     * Traverses the parent chain to ensure no circular parent relationship is formed.
     */
    private void assertNoCircularParent(String parentItemId, String currentItemId) {
        String cursor = parentItemId;
        while (cursor != null && !cursor.isBlank()) {
            BizAssert.state(!currentItemId.equals(cursor), BaseError.INVALID_PARAMETER);
            DictItem parent = dictItemRepository.findById(cursor).orElse(null);
            if (parent == null) {
                return;
            }
            cursor = parent.getParentItemId();
        }
    }

    private String normalizeCode(String code) {
        BizAssert.notBlank(code, BaseError.MISSING_PARAMETER);
        String trimmed = code.trim();
        BizAssert.state(trimmed.length() <= TYPE_CODE_MAX, BaseError.INVALID_PARAMETER);
        BizAssert.state(trimmed.matches("^[A-Za-z0-9_\\-.:/]+$"), BaseError.INVALID_PARAMETER);
        return trimmed;
    }

    private String normalizeClientKey(String clientKey) {
        BizAssert.notBlank(clientKey, BaseError.MISSING_PARAMETER);
        String trimmed = clientKey.trim();
        BizAssert.state(trimmed.length() <= 128, BaseError.INVALID_PARAMETER);
        return trimmed;
    }

    private String normalizeItemCode(String code) {
        BizAssert.notBlank(code, BaseError.MISSING_PARAMETER);
        String trimmed = code.trim();
        BizAssert.state(trimmed.length() <= ITEM_CODE_MAX, BaseError.INVALID_PARAMETER);
        BizAssert.state(trimmed.matches("^[A-Za-z0-9_\\-.:/]+$"), BaseError.INVALID_PARAMETER);
        return trimmed;
    }

    private String normalizeName(String value) {
        BizAssert.notBlank(value, BaseError.MISSING_PARAMETER);
        String trimmed = value.trim();
        BizAssert.state(trimmed.length() <= TYPE_NAME_MAX, BaseError.INVALID_PARAMETER);
        return trimmed;
    }

    private String normalizeItemLabel(String value) {
        BizAssert.notBlank(value, BaseError.MISSING_PARAMETER);
        String trimmed = value.trim();
        BizAssert.state(trimmed.length() <= ITEM_LABEL_MAX, BaseError.INVALID_PARAMETER);
        return trimmed;
    }

    private String normalizeItemValue(String value) {
        BizAssert.notBlank(value, BaseError.MISSING_PARAMETER);
        String trimmed = value.trim();
        BizAssert.state(trimmed.length() <= ITEM_VALUE_MAX, BaseError.INVALID_PARAMETER);
        return trimmed;
    }

    private String normalizeItemValue(DictValueType valueType, String value) {
        String trimmed = normalizeItemValue(value);
        return switch (defaultValueType(valueType)) {
            case STRING -> trimmed;
            case NUMBER -> normalizeNumberItemValue(trimmed);
            case BOOLEAN -> normalizeBooleanItemValue(trimmed);
        };
    }

    private String normalizeNumberItemValue(String value) {
        BizAssert.state(value.matches("^-?(0|[1-9]\\d*)(\\.\\d+)?$"), BaseError.INVALID_PARAMETER);
        return value;
    }

    private String normalizeBooleanItemValue(String value) {
        BizAssert.state("true".equals(value) || "false".equals(value), BaseError.INVALID_PARAMETER);
        return value;
    }

    private boolean normalizeItemEnabled(boolean enabled, boolean defaultItem) {
        BizAssert.state(!defaultItem || enabled, BaseError.INVALID_PARAMETER);
        return defaultItem || enabled;
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        BizAssert.state(trimmed.length() <= maxLength, BaseError.INVALID_PARAMETER);
        return trimmed;
    }

    private int normalizeSortNo(Integer sortNo) {
        return sortNo == null || sortNo < 0 ? 0 : sortNo;
    }

    private int normalizeSubmittedSortNo(Integer sortNo, int defaultValue) {
        return sortNo == null || sortNo < 0 ? defaultValue : sortNo;
    }

    private DictValueType defaultValueType(DictValueType valueType) {
        return valueType == null ? DictValueType.STRING : valueType;
    }

    private DictStructureType defaultStructureType(DictStructureType structureType) {
        return structureType == null ? DictStructureType.FLAT : structureType;
    }

    private String operator() {
        String username = CtxUtil.getPrincipal().username();
        return username == null || username.isBlank() ? DefaultUser.SYSTEM.account() : username.trim();
    }

    private String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DictTypeView toTypeView(DictType item) {
        return new DictTypeView(item.getId(), item.getCode(), item.getName(), item.getDescription(),
                item.getEnumClass(), item.getValueType(), item.getStructureType(), item.getSourceType(),
                item.isEnabled());
    }

    private DictItemView toItemView(DictItem item) {
        return new DictItemView(item.getId(), item.getDictTypeId(), item.getParentItemId(), item.getItemCode(),
                item.getItemLabel(), item.getItemValue(), item.getSortNo(), item.isEnabled(), item.isDefaultItem(),
                item.getTagColor(), item.getTagType(), item.getExtraJson(), item.getDescription());
    }

    private record DraftItemPayload(
            String id,
            String clientKey,
            String parentClientKey,
            String itemCode,
            String itemLabel,
            String itemValue,
            Integer sortNo,
            boolean enabled,
            boolean defaultItem,
            String tagColor,
            String tagType,
            String extraJson,
            String description
    ) {
    }
}

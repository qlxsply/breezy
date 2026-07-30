package com.corwin.system.dict.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.Instant;

/**
 * JPA entity representing a dictionary item.
 * <p>A dictionary item belongs to a dictionary type and carries a code, label, value, sorting order, and optional hierarchical relation via parentItemId.</p>
 *
 * @author Corwin 2026/3/15
 */
@Getter
@Entity
@Table(name = "sys_dict_item",
        uniqueConstraints = {@UniqueConstraint(name = "uk_sys_dict_item_code",
                columnNames = {"dict_type_id", "item_code"})},
        indexes = {@Index(name = "idx_sys_dict_item_type", columnList = "dict_type_id"),
                @Index(name = "idx_sys_dict_item_parent", columnList = "parent_item_id"),
                @Index(name = "idx_sys_dict_item_enabled", columnList = "enabled")})
public class DictItem {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "dict_type_id", nullable = false, length = 64)
    private String dictTypeId;

    @Column(name = "parent_item_id", length = 64)
    private String parentItemId;

    @Column(name = "item_code", nullable = false, length = 128)
    private String itemCode;

    @Column(name = "item_label", nullable = false, length = 128)
    private String itemLabel;

    @Column(name = "item_value", nullable = false, length = 512)
    private String itemValue;

    @Column(name = "sort_no", nullable = false)
    private Integer sortNo;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "is_default", nullable = false)
    private boolean defaultItem;

    @Column(name = "tag_color", length = 32)
    private String tagColor;

    @Column(name = "tag_type", length = 32)
    private String tagType;

    @Column(name = "extra_json", length = 4000)
    private String extraJson;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, length = 64)
    private String updatedBy;

    protected DictItem() {
    }

    /**
     * Constructs a new DictItem entity with the given attributes.
     */
    public DictItem(String id, String dictTypeId, String parentItemId, String itemCode, String itemLabel,
            String itemValue, Integer sortNo, boolean enabled, boolean defaultItem, String tagColor, String tagType,
            String extraJson, String description, String operator) {
        this.id = id;
        this.dictTypeId = dictTypeId;
        this.parentItemId = parentItemId;
        this.itemCode = itemCode;
        this.itemLabel = itemLabel;
        this.itemValue = itemValue;
        this.sortNo = sortNo;
        this.enabled = enabled;
        this.defaultItem = defaultItem;
        this.tagColor = tagColor;
        this.tagType = tagType;
        this.extraJson = extraJson;
        this.description = description;
        this.createdAt = HighDate.mockInstant();
        this.updatedAt = this.createdAt;
        this.createdBy = operator;
        this.updatedBy = operator;
    }

    /**
     * Updates the mutable fields of this dictionary item.
     */
    public void update(String parentItemId, String itemLabel, String itemValue, Integer sortNo, boolean enabled,
            boolean defaultItem, String tagColor, String tagType, String extraJson, String description,
            String operator) {
        this.parentItemId = parentItemId;
        this.itemLabel = itemLabel;
        this.itemValue = itemValue;
        this.sortNo = sortNo;
        this.enabled = enabled;
        this.defaultItem = defaultItem;
        this.tagColor = tagColor;
        this.tagType = tagType;
        this.extraJson = extraJson;
        this.description = description;
        touch(operator);
    }

    /**
     * Enables this dictionary item.
     */
    public void enable(String operator) {
        this.enabled = true;
        touch(operator);
    }

    /**
     * Disables this dictionary item.
     */
    public void disable(String operator) {
        this.enabled = false;
        touch(operator);
    }

    /**
     * Updates the sort order of this dictionary item.
     */
    public void updateSortNo(Integer sortNo, String operator) {
        this.sortNo = sortNo;
        touch(operator);
    }

    /**
     * Marks this item as the default item of its dictionary type.
     */
    public void markDefault(String operator) {
        this.defaultItem = true;
        touch(operator);
    }

    /**
     * Clears the default flag of this item.
     */
    public void clearDefault(String operator) {
        this.defaultItem = false;
        touch(operator);
    }

    private void touch(String operator) {
        this.updatedAt = HighDate.mockInstant();
        this.updatedBy = operator;
    }
}

package com.corwin.system.dict.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * Enum representing the structure type of a dictionary type.
 *
 * <p>Supported types: FLAT (flat list), TREE (hierarchical tree).
 *
 * @author Corwin 2026/3/15
 */
public enum DictStructureType implements DictEnumDefinition {
  FLAT("平铺结构", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
  TREE("树形结构", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  DictStructureType(String label, DictTagColor tagColor, DictTagType tagType) {
    this.label = label;
    this.tagColor = tagColor;
    this.tagType = tagType;
  }

  @Override
  public String label() {
    return label;
  }

  @Override
  public String tagColor() {
    return tagColor.itemValue();
  }

  @Override
  public String tagType() {
    return tagType.itemValue();
  }
}

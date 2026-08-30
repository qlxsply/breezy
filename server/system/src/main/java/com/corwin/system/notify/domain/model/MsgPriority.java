package com.corwin.system.notify.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * Message priority enumeration (LOW, MEDIUM, HIGH).
 *
 * <p>Each priority level has an associated display label, tag color, and tag type for UI rendering.
 *
 * @author Corwin 2026/3/16
 */
public enum MsgPriority implements DictEnumDefinition {
  LOW("低", DictTagColor.SLATE, DictTagType.INFO),
  MEDIUM("中", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
  HIGH("高", DictTagColor.DANGER_RED, DictTagType.DANGER);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  MsgPriority(String label, DictTagColor tagColor, DictTagType tagType) {
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

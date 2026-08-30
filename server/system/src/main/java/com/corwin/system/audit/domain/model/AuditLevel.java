package com.corwin.system.audit.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * Severity levels for audit log entries. Ranges from LOW (informational) to CRITICAL (severe), each
 * with an associated label, tag color, and tag type for UI rendering.
 *
 * @author Corwin 2026/4/19
 */
public enum AuditLevel implements DictEnumDefinition {
  LOW("低", DictTagColor.SLATE, DictTagType.INFO),
  MEDIUM("中", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
  HIGH("高", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
  CRITICAL("严重", DictTagColor.DANGER_RED, DictTagType.DANGER);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  AuditLevel(String label, DictTagColor tagColor, DictTagType tagType) {
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

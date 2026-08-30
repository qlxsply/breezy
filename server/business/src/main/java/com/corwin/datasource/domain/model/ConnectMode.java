package com.corwin.datasource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * @author Corwin 2026/3/6
 */
public enum ConnectMode implements DictEnumDefinition {
  HOST_PORT("主机端口", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),
  SERVICE_NAME("服务名", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),
  SID("SID", DictTagColor.WARNING_ORANGE, DictTagType.WARNING),
  TNS("TNS", DictTagColor.PURPLE, DictTagType.INFO);

  private final String label;
  private final DictTagColor tagColor;
  private final DictTagType tagType;

  ConnectMode(String label, DictTagColor tagColor, DictTagType tagType) {
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

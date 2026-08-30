package com.corwin.system.resource.domain.model;

import com.corwin.framework.dict.DictEnumDefinition;
import com.corwin.framework.dict.DictTagColor;
import com.corwin.framework.dict.DictTagType;

/**
 * Resource type enumeration for the system resource tree.
 *
 * <p>Defines the types of nodes in the admin resource hierarchy. Each type has specific rules about
 * which child types it can contain and whether it can bind permission codes.
 *
 * <p>Resource tree structure constraints:
 *
 * <pre>
 * DIRECTORY
 * ├── DIRECTORY
 * └── MENU
 *
 * MENU
 * ├── MENU
 * ├── FUNCTION
 * └── BUTTON
 *
 * FUNCTION
 * └── BUTTON
 *
 * BUTTON
 * └── (no children allowed)
 * </pre>
 *
 * <p>Key notes:
 *
 * <ul>
 *   <li>Permission codes are not resource tree nodes.
 *   <li>Only BUTTON-type resources can bind permissions.
 *   <li>Permission bindings are managed via {@link ResourcePermission}.
 * </ul>
 *
 * @author Corwin
 */
public enum ResourceType implements DictEnumDefinition {

  /**
   * Directory: a navigational grouping node.
   *
   * <p>Does not correspond to a page. Can only contain sub-directories or menus.
   */
  DIRECTORY("目录", DictTagColor.SLATE, DictTagType.INFO),

  /**
   * Menu: a navigational entry typically linked to a front-end route.
   *
   * <p>Can contain sub-menus, functions, or buttons.
   */
  MENU("菜单", DictTagColor.PRIMARY_BLUE, DictTagType.INFO),

  /**
   * Function: a sub-page capability or feature group under a menu.
   *
   * <p>Can only contain buttons.
   */
  FUNCTION("功能", DictTagColor.SUCCESS_GREEN, DictTagType.SUCCESS),

  /**
   * Button: the leaf-level UI action node.
   *
   * <p>No children allowed. Only buttons can bind permission codes.
   */
  BUTTON("按钮", DictTagColor.WARNING_ORANGE, DictTagType.WARNING);

  private final String label;

  private final DictTagColor tagColor;

  private final DictTagType tagType;

  ResourceType(String label, DictTagColor tagColor, DictTagType tagType) {
    this.label = label;
    this.tagColor = tagColor;
    this.tagType = tagType;
  }

  /**
   * Checks whether this resource type can have a child of the given type.
   *
   * @param childType the child resource type to check
   * @return true if this type can contain the specified child type
   */
  public boolean canHaveChild(ResourceType childType) {
    if (childType == null) {
      return false;
    }

    return switch (this) {
      case DIRECTORY -> childType == DIRECTORY || childType == MENU;
      case MENU -> childType == MENU || childType == FUNCTION || childType == BUTTON;
      case FUNCTION -> childType == BUTTON;
      case BUTTON -> false;
    };
  }

  /**
   * Checks whether this resource type can have any child resources.
   *
   * @return true if this type can have children
   */
  public boolean canHaveChildren() {
    return this != BUTTON;
  }

  /**
   * Checks whether this resource type can bind permission codes.
   *
   * <p>In the current model, only BUTTON-type resources can bind permissions.
   *
   * @return true if this type can bind permission codes
   */
  public boolean canBindPermission() {
    return this == BUTTON;
  }

  /**
   * Checks whether this resource type is a navigational resource.
   *
   * <p>DIRECTORY and MENU typically participate in sidebar or top navigation rendering.
   *
   * @return true if this type is navigational
   */
  public boolean isNavigationResource() {
    return this == DIRECTORY || this == MENU;
  }

  /**
   * Checks whether this resource type is page-like (menu or function).
   *
   * <p>MENU represents a page entry point. FUNCTION represents a sub-page or feature group.
   *
   * @return true if this type is page-like
   */
  public boolean isPageLikeResource() {
    return this == MENU || this == FUNCTION;
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

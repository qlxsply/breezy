package com.corwin.bootstrap.application.service;

import com.corwin.system.resource.domain.model.ResourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/6/29
 */
@Component
public class BootstrapResourceDefinitionLoader {

  public List<ResourceSeed> loadDefinitions(Resource xmlResource) {
    try (InputStream inputStream = xmlResource.getInputStream()) {
      XmlMapper mapper = new XmlMapper();
      ResourceRoot root = mapper.readValue(inputStream, ResourceRoot.class);
      LinkedHashSet<String> resourceCodes = new LinkedHashSet<>();
      ArrayList<ResourceSeed> result = new ArrayList<>();
      if (root.getDirectories() != null) {
        for (NavNode directory : root.getDirectories()) {
          result.add(toDirectorySeed(directory, resourceCodes));
        }
      }
      if (root.getMenus() != null) {
        for (NavNode menu : root.getMenus()) {
          result.add(toMenuSeed(menu, resourceCodes));
        }
      }
      return List.copyOf(result);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Load resource definitions failed: " + xmlResource.getFilename(), e);
    }
  }

  private ResourceSeed toDirectorySeed(NavNode node, Set<String> resourceCodes) {
    validateNavNode(node, resourceCodes, "directory");
    ArrayList<ResourceSeed> children = new ArrayList<>();
    if (node.getDirectories() != null) {
      for (NavNode child : node.getDirectories()) {
        children.add(toDirectorySeed(child, resourceCodes));
      }
    }
    if (node.getMenus() != null) {
      for (NavNode child : node.getMenus()) {
        children.add(toMenuSeed(child, resourceCodes));
      }
    }
    return new ResourceSeed(
        node.getCode().trim(),
        node.getName().trim(),
        ResourceType.DIRECTORY,
        null,
        null,
        trimToNull(node.getIcon()),
        defaultInt(node.getSortNo()),
        defaultBoolean(node.getVisible(), true),
        defaultBoolean(node.getEnabled(), true),
        false,
        trimToNull(node.getRemark()),
        List.of(),
        List.copyOf(children));
  }

  private ResourceSeed toMenuSeed(NavNode node, Set<String> resourceCodes) {
    validateNavNode(node, resourceCodes, "menu");
    ArrayList<ResourceSeed> children = new ArrayList<>();
    if (node.getMenus() != null) {
      for (NavNode child : node.getMenus()) {
        children.add(toMenuSeed(child, resourceCodes));
      }
    }
    if (node.getFunctions() != null) {
      int index = 0;
      for (FunctionNode functionNode : node.getFunctions()) {
        children.add(toFunctionSeed(functionNode, resourceCodes, index++));
      }
    }
    if (node.getButtons() != null) {
      for (ButtonNode buttonNode : node.getButtons()) {
        children.add(toButtonSeed(buttonNode, resourceCodes));
      }
    }
    return new ResourceSeed(
        node.getCode().trim(),
        node.getName().trim(),
        ResourceType.MENU,
        trimToNull(node.getPath()),
        trimToNull(node.getComponent()),
        trimToNull(node.getIcon()),
        defaultInt(node.getSortNo()),
        defaultBoolean(node.getVisible(), true),
        defaultBoolean(node.getEnabled(), true),
        false,
        trimToNull(node.getRemark()),
        List.of(),
        List.copyOf(children));
  }

  private ResourceSeed toFunctionSeed(FunctionNode node, Set<String> resourceCodes, int index) {
    validateFunction(node, resourceCodes);
    ArrayList<ResourceSeed> children = new ArrayList<>();
    if (node.getButtons() != null) {
      for (ButtonNode buttonNode : node.getButtons()) {
        children.add(toButtonSeed(buttonNode, resourceCodes));
      }
    }
    return new ResourceSeed(
        node.getCode().trim(),
        node.getName().trim(),
        ResourceType.FUNCTION,
        null,
        null,
        null,
        (index + 1) * 10,
        true,
        true,
        false,
        trimToNull(node.getDescription()),
        List.of(),
        List.copyOf(children));
  }

  private ResourceSeed toButtonSeed(ButtonNode node, Set<String> resourceCodes) {
    validateButton(node, resourceCodes);
    return new ResourceSeed(
        node.getCode().trim(),
        node.getName().trim(),
        ResourceType.BUTTON,
        null,
        null,
        null,
        defaultInt(node.getSortNo()),
        defaultBoolean(node.getVisible(), true),
        true,
        false,
        trimToNull(node.getDescription()),
        normalizePermissionCodes(node.getPermissions()),
        List.of());
  }

  private void validateNavNode(NavNode node, Set<String> resourceCodes, String type) {
    if (node.getCode() == null || node.getCode().isBlank()) {
      throw new IllegalStateException(type + " code must not be blank");
    }
    if (!resourceCodes.add(node.getCode().trim())) {
      throw new IllegalStateException("Duplicate resource code: " + node.getCode());
    }
    if (node.getName() == null || node.getName().isBlank()) {
      throw new IllegalStateException(type + " name must not be blank: " + node.getCode());
    }
  }

  private void validateFunction(FunctionNode node, Set<String> resourceCodes) {
    if (node.getCode() == null || node.getCode().isBlank()) {
      throw new IllegalStateException("function code must not be blank");
    }
    if (!resourceCodes.add(node.getCode().trim())) {
      throw new IllegalStateException("Duplicate resource code: " + node.getCode());
    }
    if (node.getName() == null || node.getName().isBlank()) {
      throw new IllegalStateException("function name must not be blank: " + node.getCode());
    }
    if (node.getButtons() == null || node.getButtons().isEmpty()) {
      throw new IllegalStateException("function buttons must not be empty: " + node.getCode());
    }
  }

  private void validateButton(ButtonNode node, Set<String> resourceCodes) {
    if (node.getCode() == null || node.getCode().isBlank()) {
      throw new IllegalStateException("button code must not be blank");
    }
    if (!resourceCodes.add(node.getCode().trim())) {
      throw new IllegalStateException("Duplicate resource code: " + node.getCode());
    }
    if (node.getName() == null || node.getName().isBlank()) {
      throw new IllegalStateException("button name must not be blank: " + node.getCode());
    }
  }

  private List<String> normalizePermissionCodes(List<PermissionNode> permissions) {
    if (permissions == null || permissions.isEmpty()) {
      return List.of();
    }
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (PermissionNode permission : permissions) {
      if (permission.getCode() == null || permission.getCode().isBlank()) {
        throw new IllegalStateException("resource permission code must not be blank");
      }
      result.add(permission.getCode().trim());
    }
    return List.copyOf(result);
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }

  private boolean defaultBoolean(Boolean value, boolean defaultValue) {
    return value == null ? defaultValue : value;
  }

  public record ResourceSeed(
      String code,
      String name,
      ResourceType resourceType,
      String path,
      String component,
      String icon,
      int sortNo,
      boolean visible,
      boolean enabled,
      boolean defaultEntry,
      String remark,
      List<String> permissionCodes,
      List<ResourceSeed> children) {}

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class ResourceRoot {
    @JacksonXmlProperty(localName = "menu")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<NavNode> menus;

    @JacksonXmlProperty(localName = "directory")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<NavNode> directories;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class NavNode {
    @JacksonXmlProperty(isAttribute = true)
    private String code;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String path;

    @JacksonXmlProperty(isAttribute = true)
    private String component;

    @JacksonXmlProperty(isAttribute = true)
    private String icon;

    @JacksonXmlProperty(isAttribute = true)
    private Integer sortNo;

    @JacksonXmlProperty(isAttribute = true)
    private Boolean visible;

    @JacksonXmlProperty(isAttribute = true)
    private Boolean enabled;

    @JacksonXmlProperty(isAttribute = true)
    private String remark;

    @JacksonXmlProperty(localName = "directory")
    @JacksonXmlElementWrapper(localName = "directories")
    private List<NavNode> directories;

    @JacksonXmlProperty(localName = "menu")
    @JacksonXmlElementWrapper(localName = "menus")
    private List<NavNode> menus;

    @JacksonXmlProperty(localName = "function")
    @JacksonXmlElementWrapper(localName = "functions")
    private List<FunctionNode> functions;

    @JacksonXmlProperty(localName = "button")
    @JacksonXmlElementWrapper(localName = "buttons")
    private List<ButtonNode> buttons;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class FunctionNode {
    @JacksonXmlProperty(isAttribute = true)
    private String code;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String description;

    @JacksonXmlProperty(localName = "button")
    @JacksonXmlElementWrapper(localName = "buttons")
    private List<ButtonNode> buttons;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class ButtonNode {
    @JacksonXmlProperty(isAttribute = true)
    private String code;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private Integer sortNo;

    @JacksonXmlProperty(isAttribute = true)
    private Boolean visible;

    @JacksonXmlProperty(localName = "description")
    private String description;

    @JacksonXmlProperty(localName = "permission")
    @JacksonXmlElementWrapper(localName = "permissions")
    private List<PermissionNode> permissions;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class PermissionNode {
    @JacksonXmlProperty(isAttribute = true)
    private String code;
  }
}

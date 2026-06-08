package com.corwin.bootstrap.application.service;

import com.corwin.system.resource.domain.model.FunctionType;
import com.corwin.system.resource.domain.model.MenuType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Corwin 2026/5/5
 */
@Component
public class BootstrapResourceDefinitionLoader {

    public List<MenuSeed> loadDefinitions(Resource xmlResource) {
        try (InputStream inputStream = xmlResource.getInputStream()) {
            XmlMapper mapper = new XmlMapper();
            ResourceRoot root = mapper.readValue(inputStream, ResourceRoot.class);
            List<NavNode> nodes = root.allNodes();
            if (nodes == null || nodes.isEmpty()) {
                return List.of();
            }
            LinkedHashSet<String> menuCodes = new LinkedHashSet<>();
            LinkedHashSet<String> functionCodes = new LinkedHashSet<>();
            List<MenuSeed> result = new ArrayList<>();
            for (NavNode node : nodes) {
                result.add(toMenuSeed(node, node.resolveNodeType(), menuCodes, functionCodes));
            }
            return List.copyOf(result);
        } catch (Exception e) {
            throw new IllegalStateException("Load resource definitions failed: " + xmlResource.getFilename(), e);
        }
    }

    private MenuSeed toMenuSeed(NavNode node, MenuType menuType, Set<String> menuCodes, Set<String> functionCodes) {
        validateMenu(node, menuCodes);
        List<MenuSeed> childMenus = new ArrayList<>();
        if (node.allNodes() != null) {
            for (NavNode child : node.allNodes()) {
                childMenus.add(toMenuSeed(child, child.resolveNodeType(), menuCodes, functionCodes));
            }
        }
        List<FunctionSeed> functions = new ArrayList<>();
        if (node.getFunctions() != null) {
            for (FunctionNode functionNode : node.getFunctions()) {
                functions.add(toFunctionSeed(functionNode, functionCodes));
            }
        }
        if (node.getButtons() != null) {
            for (ButtonNode buttonNode : node.getButtons()) {
                functions.add(toButtonSeed(buttonNode, functionCodes));
            }
        }
        return new MenuSeed(node.getCode().trim(), node.getName().trim(), trimToNull(node.getPath()),
                trimToNull(node.getComponent()), trimToNull(node.getIcon()), menuType,
                defaultInt(node.getSortNo()),
                defaultBoolean(node.getVisible(), true), trimToNull(node.getRemark()), List.copyOf(childMenus),
                List.copyOf(functions));
    }

    private FunctionSeed toFunctionSeed(FunctionNode node, Set<String> functionCodes) {
        validateFunction(node, functionCodes);
        List<String> permissionCodes = normalizePermissionCodes(node.getPermissions());
        List<FunctionSeed> children = new ArrayList<>();
        if (node.getFunctions() != null) {
            for (FunctionNode child : node.getFunctions()) {
                children.add(toFunctionSeed(child, functionCodes));
            }
        }
        return new FunctionSeed(node.getCode().trim(), node.getName().trim(), node.getType(),
                trimToNull(node.getDescription()), defaultInt(node.getSortNo()),
                defaultBoolean(node.getVisible(), true), defaultBoolean(node.getDefaultEntry(), false), permissionCodes,
                List.copyOf(children));
    }

    private FunctionSeed toButtonSeed(ButtonNode node, Set<String> functionCodes) {
        validateButton(node, functionCodes);
        List<String> permissionCodes = normalizePermissionCodes(node.getPermissions());
        return new FunctionSeed(node.getCode().trim(), node.getName().trim(), FunctionType.BUTTON,
                trimToNull(node.getDescription()), defaultInt(node.getSortNo()),
                defaultBoolean(node.getVisible(), true), false, permissionCodes, List.of());
    }

    private void validateMenu(NavNode node, Set<String> menuCodes) {
        if (node.getCode() == null || node.getCode().isBlank()) {
            throw new IllegalStateException("menu code must not be blank");
        }
        if (!menuCodes.add(node.getCode().trim())) {
            throw new IllegalStateException("Duplicate menu code: " + node.getCode());
        }
        if (node.getName() == null || node.getName().isBlank()) {
            throw new IllegalStateException("menu name must not be blank: " + node.getCode());
        }
    }

    private void validateFunction(FunctionNode node, Set<String> functionCodes) {
        if (node.getCode() == null || node.getCode().isBlank()) {
            throw new IllegalStateException("function code must not be blank");
        }
        if (!functionCodes.add(node.getCode().trim())) {
            throw new IllegalStateException("Duplicate function code: " + node.getCode());
        }
        if (node.getName() == null || node.getName().isBlank()) {
            throw new IllegalStateException("function name must not be blank: " + node.getCode());
        }
        if (node.getType() == null) {
            throw new IllegalStateException("function type must not be blank: " + node.getCode());
        }
    }

    private void validateButton(ButtonNode node, Set<String> functionCodes) {
        if (node.getCode() == null || node.getCode().isBlank()) {
            throw new IllegalStateException("button code must not be blank");
        }
        if (!functionCodes.add(node.getCode().trim())) {
            throw new IllegalStateException("Duplicate function code: " + node.getCode());
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

    public record MenuSeed(
            String code,
            String name,
            String path,
            String component,
            String icon,
            MenuType menuType,
            int sortNo,
            boolean visible,
            String remark,
            List<MenuSeed> children,
            List<FunctionSeed> functions
    ) {
    }

    public record FunctionSeed(
            String code,
            String name,
            FunctionType functionType,
            String description,
            int sortNo,
            boolean visible,
            boolean defaultEntry,
            List<String> permissionCodes,
            List<FunctionSeed> children
    ) {
    }

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

        private List<NavNode> allNodes() {
            List<NavNode> nodes = new ArrayList<>();
            if (directories != null) {
                nodes.addAll(directories);
            }
            if (menus != null) {
                nodes.addAll(menus);
            }
            return nodes;
        }
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
        private MenuType menuType;

        @JacksonXmlProperty(isAttribute = true)
        private Integer sortNo;

        @JacksonXmlProperty(isAttribute = true)
        private Boolean visible;

        @JacksonXmlProperty(localName = "remark")
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

        private List<NavNode> allNodes() {
            List<NavNode> nodes = new ArrayList<>();
            if (directories != null) {
                nodes.addAll(directories);
            }
            if (menus != null) {
                nodes.addAll(menus);
            }
            return nodes;
        }

        private MenuType resolveNodeType() {
            if (menuType != null) {
                return menuType;
            }
            return (path == null || path.isBlank()) && (component == null || component.isBlank())
                    ? MenuType.DIRECTORY
                    : MenuType.MENU;
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FunctionNode {
        @JacksonXmlProperty(isAttribute = true)
        private String code;

        @JacksonXmlProperty(isAttribute = true)
        private String name;

        @JacksonXmlProperty(isAttribute = true, localName = "type")
        private FunctionType type;

        @JacksonXmlProperty(isAttribute = true)
        private Integer sortNo;

        @JacksonXmlProperty(isAttribute = true)
        private Boolean visible;

        @JacksonXmlProperty(isAttribute = true)
        private Boolean defaultEntry;

        @JacksonXmlProperty(localName = "description")
        private String description;

        @JacksonXmlProperty(localName = "permission")
        @JacksonXmlElementWrapper(localName = "permissions")
        private List<PermissionNode> permissions;

        @JacksonXmlProperty(localName = "function")
        @JacksonXmlElementWrapper(localName = "functions")
        private List<FunctionNode> functions;
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

package com.corwin.bootstrap.application.service;

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
public class BootstrapNormalFeatureDefinitionLoader {

    public List<FeatureSeed> loadDefinitions(Resource xmlResource) {
        try (InputStream inputStream = xmlResource.getInputStream()) {
            XmlMapper mapper = new XmlMapper();
            FeatureRoot root = mapper.readValue(inputStream, FeatureRoot.class);
            List<FeatureNode> nodes = root.getFeatures();
            if (nodes == null || nodes.isEmpty()) {
                return List.of();
            }
            LinkedHashSet<String> featureCodes = new LinkedHashSet<>();
            List<FeatureSeed> result = new ArrayList<>();
            for (FeatureNode node : nodes) {
                validateFeature(node, featureCodes);
                result.add(
                        new FeatureSeed(node.getCode().trim(), node.getName().trim(), trimToNull(node.getDescription()),
                                Boolean.TRUE.equals(node.getEnabled()), normalizePermissionCodes(node.getPermissions()),
                                node.getSortNo() == null ? 0 : node.getSortNo()));
            }
            return List.copyOf(result);
        } catch (Exception e) {
            throw new IllegalStateException("Load normal feature definitions failed: " + xmlResource.getFilename(), e);
        }
    }

    private void validateFeature(FeatureNode node, Set<String> featureCodes) {
        if (node.getCode() == null || node.getCode().isBlank()) {
            throw new IllegalStateException("normal feature code must not be blank");
        }
        if (!featureCodes.add(node.getCode().trim())) {
            throw new IllegalStateException("Duplicate normal feature code: " + node.getCode());
        }
        if (node.getName() == null || node.getName().isBlank()) {
            throw new IllegalStateException("normal feature name must not be blank: " + node.getCode());
        }
    }

    private List<String> normalizePermissionCodes(List<PermissionNode> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (PermissionNode permission : permissions) {
            if (permission.getCode() == null || permission.getCode().isBlank()) {
                throw new IllegalStateException("normal feature permission code must not be blank");
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

    public record FeatureSeed(
            String code,
            String name,
            String description,
            boolean enabled,
            List<String> permissionCodes,
            int sortNo
    ) {
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FeatureRoot {
        @JacksonXmlProperty(localName = "feature")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<FeatureNode> features;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FeatureNode {
        @JacksonXmlProperty(isAttribute = true)
        private String code;

        @JacksonXmlProperty(isAttribute = true)
        private String name;

        @JacksonXmlProperty(isAttribute = true)
        private Boolean enabled;

        @JacksonXmlProperty(isAttribute = true)
        private Integer sortNo;

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

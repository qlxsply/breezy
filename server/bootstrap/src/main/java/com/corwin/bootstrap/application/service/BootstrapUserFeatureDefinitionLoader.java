package com.corwin.bootstrap.application.service;

import com.corwin.system.userfeature.domain.model.ApplicationFeatureAccessScope;
import com.corwin.system.userfeature.domain.model.UserApplicationPackageType;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Corwin 2026/6/14
 */
@Component
public class BootstrapUserFeatureDefinitionLoader {

    public UserFeatureDefinitionSeed loadDefinitions(Resource xmlResource) {
        try (InputStream inputStream = xmlResource.getInputStream()) {
            XmlMapper mapper = new XmlMapper();
            UserFeatureRoot root = mapper.readValue(inputStream, UserFeatureRoot.class);
            List<ApplicationNode> applicationNodes = root.getApplications();
            List<PackageNode> packageNodes = root.getPackages();

            Map<String, ApplicationSeed> applicationByCode = new LinkedHashMap<>();
            LinkedHashSet<String> applicationCodes = new LinkedHashSet<>();
            if (applicationNodes != null) {
                for (ApplicationNode node : applicationNodes) {
                    ApplicationSeed seed = toApplicationSeed(node, applicationCodes);
                    applicationByCode.put(seed.code(), seed);
                }
            }

            List<PackageSeed> packages = new ArrayList<>();
            if (packageNodes != null) {
                LinkedHashSet<String> packageCodes = new LinkedHashSet<>();
                for (PackageNode node : packageNodes) {
                    packages.add(toPackageSeed(node, packageCodes, applicationByCode));
                }
            }
            return new UserFeatureDefinitionSeed(List.copyOf(applicationByCode.values()), List.copyOf(packages));
        } catch (Exception e) {
            throw new IllegalStateException("Load user feature definitions failed: " + xmlResource.getFilename(), e);
        }
    }

    private ApplicationSeed toApplicationSeed(ApplicationNode node, Set<String> seenCodes) {
        String code = required(node.getCode(), "application code");
        if (!seenCodes.add(code)) {
            throw new IllegalStateException("Duplicate application code: " + code);
        }
        String name = required(node.getName(), "application name");
        String routePath = required(node.getRoutePath(), "application routePath");
        String componentPath = required(node.getComponentPath(), "application componentPath");

        LinkedHashSet<String> featureCodes = new LinkedHashSet<>();
        List<FeatureSeed> features = new ArrayList<>();
        if (node.getFeatures() != null) {
            for (FeatureNode featureNode : node.getFeatures()) {
                String featureCode = required(featureNode.getCode(), "feature code");
                if (!featureCodes.add(featureCode)) {
                    throw new IllegalStateException("Duplicate feature code under application " + code + ": " + featureCode);
                }
                features.add(new FeatureSeed(featureCode, required(featureNode.getName(), "feature name"), trimToNull(featureNode.getDescription()),
                        !Boolean.FALSE.equals(featureNode.getEnabled()), normalizePermissionCodes(featureNode.getPermissions()),
                        featureNode.getSortNo() == null ? 0 : featureNode.getSortNo()));
            }
        }
        return new ApplicationSeed(code, name, trimToNull(node.getDescription()), trimToNull(node.getIcon()), routePath,
                componentPath, !Boolean.FALSE.equals(node.getEnabled()), node.getSortNo() == null ? 0 : node.getSortNo(),
                List.copyOf(features));
    }

    private PackageSeed toPackageSeed(PackageNode node, Set<String> seenCodes, Map<String, ApplicationSeed> applicationByCode) {
        String code = required(node.getCode(), "package code");
        if (!seenCodes.add(code)) {
            throw new IllegalStateException("Duplicate package code: " + code);
        }
        String name = required(node.getName(), "package name");
        UserApplicationPackageType packageType = node.getPackageType() == null ? UserApplicationPackageType.CUSTOM : node.getPackageType();
        List<PackageApplicationSeed> applications = new ArrayList<>();
        LinkedHashSet<String> applicationCodes = new LinkedHashSet<>();
        if (node.getApplications() != null) {
            for (PackageApplicationNode applicationNode : node.getApplications()) {
                String applicationCode = required(applicationNode.getCode(), "package application code");
                if (!applicationCodes.add(applicationCode)) {
                    throw new IllegalStateException("Duplicate package application code under package " + code + ": " + applicationCode);
                }
                ApplicationSeed application = applicationByCode.get(applicationCode);
                if (application == null) {
                    throw new IllegalStateException("Package application not found: " + applicationCode);
                }
                ApplicationFeatureAccessScope scope = applicationNode.getFeatureAccessScope() == null
                        ? ApplicationFeatureAccessScope.FULL
                        : applicationNode.getFeatureAccessScope();
                List<String> featureCodes = normalizeFeatureCodes(applicationNode.getFeatures(), application);
                applications.add(new PackageApplicationSeed(applicationCode, scope, featureCodes));
            }
        }
        return new PackageSeed(code, name, packageType, trimToNull(node.getDescription()), !Boolean.FALSE.equals(node.getEnabled()),
                Boolean.TRUE.equals(node.getDefaultPackage()), node.getSortNo() == null ? 0 : node.getSortNo(), List.copyOf(applications));
    }

    private List<String> normalizeFeatureCodes(List<FeatureRefNode> featureRefs, ApplicationSeed application) {
        if (featureRefs == null || featureRefs.isEmpty()) {
            return List.of();
        }
        Set<String> allowedCodes = application.features().stream().map(FeatureSeed::code).collect(java.util.stream.Collectors.toSet());
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (FeatureRefNode featureRef : featureRefs) {
            String code = required(featureRef.getCode(), "package feature code");
            if (!allowedCodes.contains(code)) {
                throw new IllegalStateException("Package feature code not found under application " + application.code() + ": " + code);
            }
            result.add(code);
        }
        return List.copyOf(result);
    }

    private List<String> normalizePermissionCodes(List<PermissionNode> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (PermissionNode permission : permissions) {
            result.add(required(permission.getCode(), "permission code"));
        }
        return List.copyOf(result);
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(label + " must not be blank");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record UserFeatureDefinitionSeed(
            List<ApplicationSeed> applications,
            List<PackageSeed> packages
    ) {
    }

    public record ApplicationSeed(
            String code,
            String name,
            String description,
            String icon,
            String routePath,
            String componentPath,
            boolean enabled,
            int sortNo,
            List<FeatureSeed> features
    ) {
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

    public record PackageSeed(
            String code,
            String name,
            UserApplicationPackageType packageType,
            String description,
            boolean enabled,
            boolean defaultPackage,
            int sortNo,
            List<PackageApplicationSeed> applications
    ) {
    }

    public record PackageApplicationSeed(
            String applicationCode,
            ApplicationFeatureAccessScope featureAccessScope,
            List<String> featureCodes
    ) {
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UserFeatureRoot {
        @JacksonXmlProperty(localName = "application")
        @JacksonXmlElementWrapper(localName = "applications")
        private List<ApplicationNode> applications;

        @JacksonXmlProperty(localName = "package")
        @JacksonXmlElementWrapper(localName = "packages")
        private List<PackageNode> packages;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ApplicationNode {
        @JacksonXmlProperty(isAttribute = true) private String code;
        @JacksonXmlProperty(isAttribute = true) private String name;
        @JacksonXmlProperty(isAttribute = true) private Boolean enabled;
        @JacksonXmlProperty(isAttribute = true) private Integer sortNo;
        @JacksonXmlProperty(isAttribute = true) private String icon;
        @JacksonXmlProperty(isAttribute = true) private String routePath;
        @JacksonXmlProperty(isAttribute = true) private String componentPath;
        @JacksonXmlProperty(localName = "description") private String description;
        @JacksonXmlProperty(localName = "feature")
        @JacksonXmlElementWrapper(localName = "features")
        private List<FeatureNode> features;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FeatureNode {
        @JacksonXmlProperty(isAttribute = true) private String code;
        @JacksonXmlProperty(isAttribute = true) private String name;
        @JacksonXmlProperty(isAttribute = true) private Boolean enabled;
        @JacksonXmlProperty(isAttribute = true) private Integer sortNo;
        @JacksonXmlProperty(localName = "description") private String description;
        @JacksonXmlProperty(localName = "permission")
        @JacksonXmlElementWrapper(localName = "permissions")
        private List<PermissionNode> permissions;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PackageNode {
        @JacksonXmlProperty(isAttribute = true) private String code;
        @JacksonXmlProperty(isAttribute = true) private String name;
        @JacksonXmlProperty(isAttribute = true) private Boolean enabled;
        @JacksonXmlProperty(isAttribute = true) private Boolean defaultPackage;
        @JacksonXmlProperty(isAttribute = true) private Integer sortNo;
        @JacksonXmlProperty(isAttribute = true) private UserApplicationPackageType packageType;
        @JacksonXmlProperty(localName = "description") private String description;
        @JacksonXmlProperty(localName = "application")
        @JacksonXmlElementWrapper(localName = "applications")
        private List<PackageApplicationNode> applications;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PackageApplicationNode {
        @JacksonXmlProperty(isAttribute = true) private String code;
        @JacksonXmlProperty(isAttribute = true) private ApplicationFeatureAccessScope featureAccessScope;
        @JacksonXmlProperty(localName = "feature")
        @JacksonXmlElementWrapper(localName = "features")
        private List<FeatureRefNode> features;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FeatureRefNode {
        @JacksonXmlProperty(isAttribute = true) private String code;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PermissionNode {
        @JacksonXmlProperty(isAttribute = true) private String code;
    }
}

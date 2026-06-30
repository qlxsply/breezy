package com.corwin.bootstrap.application.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Corwin 2026/6/30
 */
@Component
public class BootstrapPermissionNameDefinitionLoader {

    private final BootstrapDefinitionResources definitionResources;

    public BootstrapPermissionNameDefinitionLoader(BootstrapDefinitionResources definitionResources) {
        this.definitionResources = definitionResources;
    }

    public Map<String, String> load() {
        Resource resource = definitionResources.permissionsProperties();
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0 || separatorIndex == trimmed.length() - 1) {
                    continue;
                }
                String normalizedCode = normalize(trimmed.substring(0, separatorIndex));
                String normalizedName = normalize(trimmed.substring(separatorIndex + 1));
                if (normalizedCode == null || normalizedName == null) {
                    continue;
                }
                result.put(normalizedCode, normalizedName);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Load permission name definitions failed", e);
        }
        return Map.copyOf(result);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

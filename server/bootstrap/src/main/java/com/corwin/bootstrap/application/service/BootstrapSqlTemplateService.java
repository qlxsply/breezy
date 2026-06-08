package com.corwin.bootstrap.application.service;

import com.corwin.bootstrap.application.DatabaseType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Corwin 2026/4/28
 */
@Component
@RequiredArgsConstructor
public class BootstrapSqlTemplateService {

    private final BootstrapDatabaseTypeResolver databaseTypeResolver;

    public String load(String relativePath) {
        DatabaseType databaseType = databaseTypeResolver.resolveCurrentDatabaseType();
        String path = "bootstarp/sql/" + BootstrapJdbcUrlSupport.toFolder(databaseType) + "/" + relativePath;
        return loadClasspathResource(path);
    }

    private String loadClasspathResource(String path) {
        try (var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("SQL resource not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Load SQL resource failed: " + path, e);
        }
    }

    public List<ScriptResource> listSchemaScripts() {
        return List.of(new ScriptResource("system_schema.sql", load("system_schema.sql")),
                new ScriptResource("business_schema.sql", load("business_schema.sql")));
    }

    public record ScriptResource(
            String name,
            String content
    ) {
    }
}

package com.corwin.system.file.infrastructure.storage;

import com.corwin.framework.config.ConfigRegistry;
import com.corwin.system.config.application.config.SystemConfigKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local filesystem storage provider.
 * <p>Implements hash-sharded directory storage logic using the first 20 characters
 * of the content hash to create a 4-level directory hierarchy.</p>
 *
 * @author Corwin 2026/2/23
 */
@Slf4j
@Component
public class LocalStorageProvider {

    /**
     * Returns the base storage directory, creating it if it does not exist.
     */
    public Path getBasePath() {
        String path = ConfigRegistry.stringV(SystemConfigKeys.STORAGE_BASE_PATH);
        Path basePath = Paths.get(path).toAbsolutePath().normalize();
        if (!Files.exists(basePath)) {
            try {
                Files.createDirectories(basePath);
            } catch (IOException e) {
                log.error("Failed to create storage base path: {}", basePath, e);
            }
        }
        return basePath;
    }

    /**
     * Returns the temporary file directory under the base storage path,
     * creating it if it does not exist.
     */
    public Path getTempPath() {
        Path tempPath = getBasePath().resolve("temp");
        if (!Files.exists(tempPath)) {
            try {
                Files.createDirectories(tempPath);
            } catch (IOException e) {
                log.error("Failed to create storage temp path: {}", tempPath, e);
            }
        }
        return tempPath;
    }

    /**
     * Writes a file to the storage directory. Creates intermediate directories as needed.
     */
    public void write(String relativePath, String fileName, InputStream stream) throws IOException {
        Path fullDir = getBasePath().resolve(relativePath);
        if (!Files.exists(fullDir)) {
            Files.createDirectories(fullDir);
        }
        Path targetFile = fullDir.resolve(fileName);
        Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Deletes a file from the storage directory.
     */
    public void delete(String relativePath, String fileName) throws IOException {
        Path targetFile = getBasePath().resolve(relativePath).resolve(fileName);
        Files.deleteIfExists(targetFile);
    }

    /**
     * Opens an input stream to read a file from the storage directory.
     */
    public InputStream read(String relativePath, String fileName) throws IOException {
        Path targetFile = getBasePath().resolve(relativePath).resolve(fileName);
        return Files.newInputStream(targetFile);
    }

    /**
     * Generates a sharded relative path from a content hash.
     * Takes the first 20 characters and groups them into 4 levels of 5 characters each.
     * Falls back to "default" if the hash is too short.
     */
    public String generateRelativePath(String hash) {
        if (hash == null || hash.length() < 20) {
            return "default";
        }
        String p1 = hash.substring(0, 5);
        String p2 = hash.substring(5, 10);
        String p3 = hash.substring(10, 15);
        String p4 = hash.substring(15, 20);
        return String.join("/", p1, p2, p3, p4);
    }
}

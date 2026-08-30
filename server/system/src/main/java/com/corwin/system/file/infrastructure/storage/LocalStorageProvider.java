package com.corwin.system.file.infrastructure.storage;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.system.file.config.SystemFileConfigSpecs;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Corwin 2026/2/23
 */
@Slf4j
@Component
public class LocalStorageProvider {

  public Path getBasePath() {
    String path = Configs.get(SystemFileConfigSpecs.STORAGE).basePath();
    Path basePath = Paths.get(path).toAbsolutePath().normalize();
    if (!Files.exists(basePath)) {
      try {
        Files.createDirectories(basePath);
      } catch (IOException ex) {
        log.error("Failed to create storage base path: {}", basePath, ex);
      }
    }
    return basePath;
  }

  public Path getTempPath() {
    Path tempPath = getBasePath().resolve("temp");
    if (!Files.exists(tempPath)) {
      try {
        Files.createDirectories(tempPath);
      } catch (IOException ex) {
        log.error("Failed to create storage temp path: {}", tempPath, ex);
      }
    }
    return tempPath;
  }

  public void write(String relativePath, String fileName, InputStream stream) throws IOException {
    Path fullDirectory = getBasePath().resolve(relativePath);
    Files.createDirectories(fullDirectory);
    Files.copy(stream, fullDirectory.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
  }

  public void delete(String relativePath, String fileName) throws IOException {
    Files.deleteIfExists(getBasePath().resolve(relativePath).resolve(fileName));
  }

  public InputStream read(String relativePath, String fileName) throws IOException {
    return Files.newInputStream(getBasePath().resolve(relativePath).resolve(fileName));
  }

  public String generateRelativePath(String hash) {
    if (hash == null || hash.length() < 20) {
      return "default";
    }
    return String.join(
        "/",
        hash.substring(0, 5),
        hash.substring(5, 10),
        hash.substring(10, 15),
        hash.substring(15, 20));
  }
}

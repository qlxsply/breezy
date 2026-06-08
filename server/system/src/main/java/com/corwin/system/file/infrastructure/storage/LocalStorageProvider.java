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
 * 本地文件系统存储提供者。
 * <p>
 * 实现摘要分片存储逻辑。
 *
 * @author Corwin 2026/2/23
 */
@Slf4j
@Component
public class LocalStorageProvider {

    /**
     * 获取基础路径
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
     * 获取临时文件目录
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
     * 写入文件
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
     * 删除文件
     */
    public void delete(String relativePath, String fileName) throws IOException {
        Path targetFile = getBasePath().resolve(relativePath).resolve(fileName);
        Files.deleteIfExists(targetFile);
    }

    /**
     * 读取文件
     */
    public InputStream read(String relativePath, String fileName) throws IOException {
        Path targetFile = getBasePath().resolve(relativePath).resolve(fileName);
        return Files.newInputStream(targetFile);
    }

    /**
     * 生成相对路径：取摘要前 20 个字符，每 5 个一组，形成 4 级目录。
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

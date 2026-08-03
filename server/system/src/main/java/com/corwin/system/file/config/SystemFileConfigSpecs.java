package com.corwin.system.file.config;

import com.corwin.framework.config.definition.*;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

import static com.corwin.framework.config.definition.ConfigFieldSpecs.required;

/**
 * @author Corwin 2026/7/31
 */
public final class SystemFileConfigSpecs {

    public static final ConfigSpec<FileStorageConfig> STORAGE = new SimpleConfigSpec<>(
            new ConfigKey("system.file.storage"), "system", "file", "文件存储", "本地文件存储根路径",
            FileStorageConfig.class, new FileStorageConfig("./data/storage"), 1, 10,
            ConfigActivationPolicy.RESTART_REQUIRED, ConfigEditPolicy.ADMIN_EDITABLE,
            ConfigInvalidValuePolicy.FAIL_STARTUP,
            List.of(required("basePath", "存储根路径", ConfigFieldType.STRING, 10)), "default",
            SystemFileConfigSpecs::validateStorage, SystemFileConfigSpecs::validateStorage);

    public static final ConfigSpec<FilePreviewPolicy> PREVIEW = new SimpleConfigSpec<>(
            new ConfigKey("system.file.preview"), "system", "file", "文件预览策略",
            "文件在线预览最大字节数，0 表示不限制", FilePreviewPolicy.class, new FilePreviewPolicy(5_242_880L), 1, 20,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(required("maxSizeBytes", "最大预览字节数", ConfigFieldType.LONG, 10)), "default",
            SystemFileConfigSpecs::validatePreview, SystemFileConfigSpecs::validatePreview);

    public record FileStorageConfig(String basePath) {
    }

    public record FilePreviewPolicy(long maxSizeBytes) {
    }

    private static List<ConfigViolation> validateStorage(FileStorageConfig value) {
        if (value.basePath() == null || value.basePath().isBlank()) {
            return List.of(new ConfigViolation("basePath", "REQUIRED", "存储根路径不能为空"));
        }
        try {
            Path.of(value.basePath());
            return List.of();
        } catch (InvalidPathException ex) {
            return List.of(new ConfigViolation("basePath", "INVALID_PATH", "存储根路径格式无效"));
        }
    }

    private static List<ConfigViolation> validatePreview(FilePreviewPolicy value) {
        return value.maxSizeBytes() >= 0 ? List.of() : List.of(
                new ConfigViolation("maxSizeBytes", "OUT_OF_RANGE", "最大预览字节数不能为负数"));
    }

    private SystemFileConfigSpecs() {
    }
}

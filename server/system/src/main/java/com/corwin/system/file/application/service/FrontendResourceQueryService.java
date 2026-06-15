package com.corwin.system.file.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.file.application.view.FrontendResourceFileView;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Corwin 2026/6/15
 */
@Service
public class FrontendResourceQueryService {

    private final FileQueryService fileQueryService;
    private final Path baseDirectory;

    public FrontendResourceQueryService(FileQueryService fileQueryService) {
        Path baseDirectory0 = null;
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add("./app-resources/public");
        candidates.add("./business/app-resources/public");
        candidates.add("./server/business/app-resources/public");
        for (String candidate : candidates) {
            Path path = Path.of(candidate).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) {
                baseDirectory0 = path;
                break;
            }
        }
        if (baseDirectory0 == null) {
            throw new BizException("资源目录不存在", BaseError.SERVICE_ERROR);
        }

        this.fileQueryService = fileQueryService;
        this.baseDirectory = baseDirectory0;
    }

    public FrontendResourceFileView getResource(String resourcePath) throws IOException {
        String normalizedResourcePath = normalizeResourcePath(resourcePath);
        Path relativePath = Path.of(normalizedResourcePath).normalize();
        Path targetPath = baseDirectory.resolve(relativePath).normalize();
        if (!targetPath.startsWith(baseDirectory)) {
            throw new BizException("资源路径非法", BaseError.ILLEGAL_ARGUMENT);
        }
        if (!Files.isRegularFile(targetPath)) {
            throw new BizException(BaseError.NOT_FOUND);
        }
        String fileName = targetPath.getFileName().toString();
        String contentType = fileQueryService.resolveContentType(fileName, Files.probeContentType(targetPath));
        return new FrontendResourceFileView(targetPath, fileName, contentType, Files.size(targetPath));
    }

    private String normalizeResourcePath(String resourcePath) {
        if (resourcePath == null) {
            throw new BizException("资源路径不能为空", BaseError.ILLEGAL_ARGUMENT);
        }
        String normalized = resourcePath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            throw new BizException("资源路径不能为空", BaseError.ILLEGAL_ARGUMENT);
        }
        Path path = Path.of(normalized).normalize();
        if (path.isAbsolute() || path.startsWith("..")) {
            throw new BizException("资源路径非法", BaseError.ILLEGAL_ARGUMENT);
        }
        return normalized;
    }
}

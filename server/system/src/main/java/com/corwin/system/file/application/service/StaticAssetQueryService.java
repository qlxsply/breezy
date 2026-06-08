package com.corwin.system.file.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.file.application.view.LogicalPhysicalFileView;
import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.repo.LogicalFileRepository;
import com.corwin.system.file.domain.repo.LogicalFolderRepository;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.OwnerType;
import com.corwin.system.user.domain.model.DefaultUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * @author Corwin 2026/5/26
 */
@Service
@RequiredArgsConstructor
public class StaticAssetQueryService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/svg+xml",
            "image/x-icon",
            "image/vnd.microsoft.icon");

    private final FileQueryService fileQueryService;
    private final LogicalFolderRepository logicalFolderRepository;
    private final LogicalFileRepository logicalFileRepository;

    public Optional<String> resolveFileIdByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return Optional.empty();
        }
        return resolveStaticAssetFolderId().flatMap(folderId -> logicalFileRepository
                .findByOwnerTypeAndOwnerIdAndParentId(OwnerType.APPLICATION, DefaultUser.SYSTEM.account(), folderId)
                .stream()
                .filter(this::isStaticAsset)
                .filter(file -> matchesCode(file.getFileName(), normalizedCode))
                .findFirst()
                .map(LogicalFile::getId));
    }

    public LogicalPhysicalFileView getStaticAssetByFileId(String fileId) {
        LogicalPhysicalFileView view = fileQueryService.getLogicalPhysicalFile(fileId);
        validateStaticAsset(view.logicalFile(),
                fileQueryService.resolveContentType(view.logicalFile().getFileName(), view.physicalFile().getContentType()));
        return view;
    }

    public void validateStaticAsset(LogicalFile logicalFile, String contentType) {
        if (logicalFile == null || !FilePurpose.STATIC_ASSET.name().equalsIgnoreCase(logicalFile.getPurpose())) {
            throw new BizException(BaseError.NOT_FOUND);
        }
        if (logicalFile.getOwnerType() != OwnerType.APPLICATION
                || !DefaultUser.SYSTEM.account().equals(logicalFile.getOwnerId())) {
            throw new BizException(BaseError.NOT_FOUND);
        }
        String normalizedType = contentType == null ? null : contentType.trim().toLowerCase(Locale.ROOT);
        if (normalizedType == null || !ALLOWED_CONTENT_TYPES.contains(normalizedType)) {
            throw new BizException("不支持的静态资源类型", BaseError.ILLEGAL_ARGUMENT);
        }
    }

    private Optional<String> resolveStaticAssetFolderId() {
        return logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentIdAndFolderName(OwnerType.APPLICATION,
                DefaultUser.SYSTEM.account(), null, FilePurpose.STATIC_ASSET.name()).map(folder -> folder.getId());
    }

    private boolean isStaticAsset(LogicalFile file) {
        return file != null && FilePurpose.STATIC_ASSET.name().equalsIgnoreCase(file.getPurpose());
    }

    private boolean matchesCode(String fileName, String code) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String normalizedFileName = fileName.trim().toLowerCase(Locale.ROOT);
        if (normalizedFileName.equals(code)) {
            return true;
        }
        int index = normalizedFileName.lastIndexOf('.');
        String baseName = index > 0 ? normalizedFileName.substring(0, index) : normalizedFileName;
        return baseName.equals(code);
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}

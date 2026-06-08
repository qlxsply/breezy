package com.corwin.system.file.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.file.application.command.StorageQueryCommand;
import com.corwin.system.file.application.command.StorageSortBy;
import com.corwin.system.file.application.command.StorageSortOrder;
import com.corwin.system.file.published.OwnerType;
import com.corwin.system.file.application.port.FileQueryPort;
import com.corwin.system.file.application.view.LogicalPhysicalFileView;
import com.corwin.system.file.application.view.StorageNodeView;
import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.model.LogicalFolder;
import com.corwin.system.file.domain.model.PhysicalFile;
import com.corwin.system.file.domain.repo.LogicalFileRepository;
import com.corwin.system.file.domain.repo.LogicalFolderRepository;
import com.corwin.system.file.domain.repo.PhysicalFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/2/23
 */
@Service
@RequiredArgsConstructor
public class FileQueryService implements FileQueryPort {

    private final LogicalFileRepository logicalFileRepository;
    private final LogicalFolderRepository logicalFolderRepository;
    private final PhysicalFileRepository physicalFileRepository;

    public List<StorageNodeView> listContent(OwnerType ownerType, String ownerId, String parentId) {
        return listContent(ownerType, ownerId,
                new StorageQueryCommand(parentId, null, false, StorageSortBy.NAME, StorageSortOrder.ASC));
    }

    @Override
    public List<StorageNodeView> listContent(OwnerType ownerType, String ownerId, StorageQueryCommand query) {
        List<LogicalFolder> folders = new ArrayList<>();
        List<LogicalFile> files = new ArrayList<>();

        collectOwnerScopedNodes(toDomainOwnerType(ownerType), ownerId, query.parentId(), query.recursive(), folders,
                files);

        List<StorageNodeView> nodes = buildNodeViews(folders, files, query.keyword());
        sortNodes(nodes, query.sortBy(), query.sortOrder());
        return nodes;
    }

    public List<StorageNodeView> listAdminContent(StorageQueryCommand query) {
        List<LogicalFolder> folders = new ArrayList<>();
        List<LogicalFile> files = new ArrayList<>();

        collectGlobalNodes(query.parentId(), query.recursive(), folders, files);

        List<StorageNodeView> nodes = buildNodeViews(folders, files, query.keyword());
        sortNodes(nodes, query.sortBy(), query.sortOrder());
        return nodes;
    }

    public List<StorageNodeView> listLogicalFileRefs(String physicalFileId, String keyword,
            StorageSortBy sortBy, StorageSortOrder sortOrder) {
        List<LogicalFile> logicalFiles = logicalFileRepository.findByPhysicalFileId(physicalFileId);
        List<StorageNodeView> nodes = buildNodeViews(List.of(), logicalFiles, keyword);
        sortNodes(nodes, sortBy, sortOrder);
        return nodes;
    }

    public LogicalPhysicalFileView getLogicalPhysicalFile(String fileId) {
        LogicalFile logicalFile = logicalFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        PhysicalFile physicalFile = physicalFileRepository.findById(logicalFile.getPhysicalFileId())
                .orElseThrow(() -> new BizException("物理文件丢失", BaseError.SERVICE_ERROR));
        return new LogicalPhysicalFileView(logicalFile, physicalFile);
    }

    @Override
    public StorageNodeView getFileMetadata(String fileId) {
        LogicalFile logicalFile = logicalFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        return toFileView(logicalFile);
    }

    @Override
    public List<StorageNodeView> getMetadataBatch(List<String> ids) {
        return logicalFileRepository.findAllById(ids).stream().map(this::toFileView).toList();
    }

    public List<StorageNodeView> listAllFileMetadata() {
        return logicalFileRepository.findAll().stream().map(this::toFileView).toList();
    }

    private void collectOwnerScopedNodes(OwnerType ownerType, String ownerId,
            String parentId, boolean recursive, List<LogicalFolder> folders, List<LogicalFile> files) {
        if (!recursive) {
            folders.addAll(logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentId(ownerType, ownerId, parentId));
            files.addAll(logicalFileRepository.findByOwnerTypeAndOwnerIdAndParentId(ownerType, ownerId, parentId));
            return;
        }

        List<String> pendingParents = new ArrayList<>();
        pendingParents.add(parentId);
        int cursor = 0;
        while (cursor < pendingParents.size()) {
            String currentParentId = pendingParents.get(cursor);
            cursor++;

            List<LogicalFolder> currentFolders = logicalFolderRepository
                    .findByOwnerTypeAndOwnerIdAndParentId(ownerType, ownerId, currentParentId);
            folders.addAll(currentFolders);
            pendingParents.addAll(currentFolders.stream().map(LogicalFolder::getId).toList());

            files.addAll(logicalFileRepository.findByOwnerTypeAndOwnerIdAndParentId(ownerType, ownerId,
                    currentParentId));
        }
    }

    private void collectGlobalNodes(String parentId, boolean recursive,
            List<LogicalFolder> folders, List<LogicalFile> files) {
        if (!recursive) {
            folders.addAll(logicalFolderRepository.findByParentId(parentId));
            files.addAll(logicalFileRepository.findByParentId(parentId));
            return;
        }

        List<String> pendingParents = new ArrayList<>();
        pendingParents.add(parentId);
        int cursor = 0;
        while (cursor < pendingParents.size()) {
            String currentParentId = pendingParents.get(cursor);
            cursor++;

            List<LogicalFolder> currentFolders = logicalFolderRepository.findByParentId(currentParentId);
            folders.addAll(currentFolders);
            pendingParents.addAll(currentFolders.stream().map(LogicalFolder::getId).toList());

            files.addAll(logicalFileRepository.findByParentId(currentParentId));
        }
    }

    private List<StorageNodeView> buildNodeViews(List<LogicalFolder> folders, List<LogicalFile> files, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Map<String, PhysicalFile> physicalMap = toPhysicalMap(files);

        List<StorageNodeView> nodes = new ArrayList<>();
        for (LogicalFolder folder : folders) {
            if (!matchKeyword(folder.getFolderName(), normalizedKeyword)) {
                continue;
            }
            nodes.add(toFolderView(folder));
        }
        for (LogicalFile file : files) {
            if (!matchKeyword(file.getFileName(), normalizedKeyword)) {
                continue;
            }
            nodes.add(toFileView(file, physicalMap));
        }
        return nodes;
    }

    private Map<String, PhysicalFile> toPhysicalMap(List<LogicalFile> files) {
        Set<String> physicalIds = files.stream().map(LogicalFile::getPhysicalFileId).collect(Collectors.toSet());
        if (physicalIds.isEmpty()) {
            return Map.of();
        }
        return physicalFileRepository.findAllById(physicalIds)
                .stream()
                .collect(Collectors.toMap(PhysicalFile::getId, Function.identity()));
    }

    private void sortNodes(List<StorageNodeView> nodes, StorageSortBy sortBy, StorageSortOrder sortOrder) {
        Comparator<StorageNodeView> comparator = buildComparator(sortBy);
        if (sortOrder == StorageSortOrder.DESC) {
            comparator = comparator.reversed();
        }
        nodes.sort(comparator);
    }

    private Comparator<StorageNodeView> buildComparator(StorageSortBy sortBy) {
        return switch (sortBy) {
            case SIZE -> Comparator.comparingLong(this::sortableSize)
                    .thenComparing(node -> normalizeName(node.name()))
                    .thenComparing(StorageNodeView::id);
            case TYPE -> Comparator.comparingInt(this::typeRank)
                    .thenComparing(node -> normalizeName(node.name()))
                    .thenComparing(StorageNodeView::id);
            case UPDATED_AT -> Comparator.comparing(this::sortableUpdatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(node -> normalizeName(node.name()))
                    .thenComparing(StorageNodeView::id);
            case NAME -> Comparator.comparing((StorageNodeView node) -> normalizeName(node.name()))
                    .thenComparing(StorageNodeView::id);
        };
    }

    private int typeRank(StorageNodeView node) {
        return "FOLDER".equals(node.type()) ? 0 : 1;
    }

    private long sortableSize(StorageNodeView node) {
        return node.size() == null ? 0L : node.size();
    }

    private LocalDateTime sortableUpdatedAt(StorageNodeView node) {
        return node.updatedAt() == null ? node.createdAt() : node.updatedAt();
    }

    private boolean matchKeyword(String sourceName, String keyword) {
        if (keyword == null) {
            return true;
        }
        if (sourceName == null) {
            return false;
        }
        return normalizeName(sourceName).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT);
    }

    public StorageNodeView toFolderView(LogicalFolder folder) {
        return new StorageNodeView(
                folder.getId(),
                "FOLDER",
                folder.getFolderName(),
                folder.getParentId(),
                toContractOwnerType(folder.getOwnerType()),
                folder.getOwnerId(),
                null,
                null,
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }

    public StorageNodeView toFileView(LogicalFile file) {
        return toFileView(file, toPhysicalMap(List.of(file)));
    }

    public StorageNodeView toFileView(LogicalFile file, Map<String, PhysicalFile> physicalMap) {
        PhysicalFile pf = physicalMap.get(file.getPhysicalFileId());
        String contentType = resolveContentType(file.getFileName(), pf != null ? pf.getContentType() : null);
        return new StorageNodeView(
                file.getId(),
                "FILE",
                file.getFileName(),
                file.getParentId(),
                toContractOwnerType(file.getOwnerType()),
                file.getOwnerId(),
                pf != null ? pf.getFileSize() : 0L,
                contentType,
                file.getCreatedAt(),
                file.getUpdatedAt()
        );
    }

    public String resolveContentType(String fileName, String contentType) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        String extension = resolveExtension(fileName);
        if (extension == null) {
            return null;
        }
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "svg" -> "image/svg+xml";
            case "ico" -> "image/x-icon";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "yaml", "yml" -> "application/yaml";
            case "csv" -> "text/csv; charset=UTF-8";
            case "md", "txt", "log", "sql" -> "text/plain; charset=UTF-8";
            default -> null;
        };
    }

    private String resolveExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private OwnerType toDomainOwnerType(OwnerType ownerType) {
        if (ownerType == null) {
            return null;
        }
        return OwnerType.valueOf(ownerType.name());
    }

    private OwnerType toContractOwnerType(OwnerType ownerType) {
        if (ownerType == null) {
            return null;
        }
        return OwnerType.valueOf(ownerType.name());
    }
}

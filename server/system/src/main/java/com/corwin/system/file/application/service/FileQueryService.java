package com.corwin.system.file.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.file.application.command.StorageQueryCommand;
import com.corwin.system.file.application.command.StorageSortBy;
import com.corwin.system.file.application.command.StorageSortOrder;
import com.corwin.system.file.application.port.FileQueryPort;
import com.corwin.system.file.application.view.LogicalPhysicalFileView;
import com.corwin.system.file.application.view.StorageNodeView;
import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.model.PhysicalFile;
import com.corwin.system.file.domain.repo.LogicalFileRepository;
import com.corwin.system.file.domain.repo.PhysicalFileRepository;
import com.corwin.system.file.published.OwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for querying and listing storage nodes.
 * Implements the {@link FileQueryPort} inbound port and provides
 * both owner-scoped and global (admin) query capabilities.
 *
 * @author Corwin 2026/2/23
 */
@Service
@RequiredArgsConstructor
public class FileQueryService implements FileQueryPort {

    private final LogicalFileRepository logicalFileRepository;
    private final PhysicalFileRepository physicalFileRepository;

    /**
     * Lists storage nodes scoped to a specific owner, with optional recursive traversal,
     * keyword filtering, and sorting.
     */
    @Override
    public List<StorageNodeView> listContent(OwnerType ownerType, String ownerId, StorageQueryCommand query) {
        List<LogicalFile> nodes = new ArrayList<>();

        collectOwnerScopedNodes(toDomainOwnerType(ownerType), ownerId, query.parentId(), query.recursive(), nodes);

        List<StorageNodeView> nodeViews = buildNodeViews(nodes, query.keyword());
        sortNodes(nodeViews, query.sortBy(), query.sortOrder());
        return nodeViews;
    }

    /**
     * Lists all storage nodes globally (admin scope) with optional recursive traversal,
     * keyword filtering, and sorting.
     */
    public List<StorageNodeView> listAdminContent(StorageQueryCommand query) {
        List<LogicalFile> nodes = new ArrayList<>();

        collectGlobalNodes(query.parentId(), query.recursive(), nodes);

        List<StorageNodeView> nodeViews = buildNodeViews(nodes, query.keyword());
        sortNodes(nodeViews, query.sortBy(), query.sortOrder());
        return nodeViews;
    }

    /**
     * Lists logical file references pointing to a given physical file.
     */
    public List<StorageNodeView> listLogicalFileRefs(String physicalFileId, String keyword, StorageSortBy sortBy,
            StorageSortOrder sortOrder) {
        List<LogicalFile> logicalFiles = logicalFileRepository.findByPhysicalFileId(physicalFileId);
        List<StorageNodeView> nodes = buildNodeViews(logicalFiles, keyword);
        sortNodes(nodes, sortBy, sortOrder);
        return nodes;
    }

    /**
     * Retrieves the logical file and its associated physical file record.
     *
     * @throws BizException if the file is not found or is a folder node
     */
    public LogicalPhysicalFileView getLogicalPhysicalFile(String fileId) {
        LogicalFile logicalFile = logicalFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        assertFileNode(logicalFile);
        PhysicalFile physicalFile = physicalFileRepository.findById(logicalFile.getPhysicalFileId())
                .orElseThrow(() -> new BizException("物理文件丢失", BaseError.SERVICE_ERROR));
        return new LogicalPhysicalFileView(logicalFile, physicalFile);
    }

    /**
     * Retrieves the metadata view for a single file by its logical file ID.
     */
    @Override
    public StorageNodeView getFileMetadata(String fileId) {
        LogicalFile logicalFile = logicalFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        assertFileNode(logicalFile);
        return toFileView(logicalFile);
    }

    /**
     * Retrieves metadata for multiple files by their logical file IDs.
     */
    @Override
    public List<StorageNodeView> getMetadataBatch(List<String> ids) {
        return logicalFileRepository.findAllById(ids).stream().filter(LogicalFile::isFile).map(this::toFileView)
                .toList();
    }

    /**
     * Lists metadata for all file nodes across the entire system.
     */
    public List<StorageNodeView> listAllFileMetadata() {
        return logicalFileRepository.findAll().stream().filter(LogicalFile::isFile).map(this::toFileView).toList();
    }

    private void collectOwnerScopedNodes(OwnerType ownerType, String ownerId, String parentId, boolean recursive,
            List<LogicalFile> nodes) {
        if (!recursive) {
            nodes.addAll(logicalFileRepository.findByOwnerTypeAndOwnerIdAndParentId(ownerType, ownerId, parentId));
            return;
        }

        List<String> pendingParents = new ArrayList<>();
        pendingParents.add(parentId);
        int cursor = 0;
        while (cursor < pendingParents.size()) {
            String currentParentId = pendingParents.get(cursor);
            cursor++;

            List<LogicalFile> currentNodes = logicalFileRepository.findByOwnerTypeAndOwnerIdAndParentId(ownerType,
                    ownerId, currentParentId);
            nodes.addAll(currentNodes);
            pendingParents.addAll(currentNodes.stream().filter(LogicalFile::isFolder).map(LogicalFile::getId).toList());
        }
    }

    private void collectGlobalNodes(String parentId, boolean recursive, List<LogicalFile> nodes) {
        if (!recursive) {
            nodes.addAll(logicalFileRepository.findByParentId(parentId));
            return;
        }

        List<String> pendingParents = new ArrayList<>();
        pendingParents.add(parentId);
        int cursor = 0;
        while (cursor < pendingParents.size()) {
            String currentParentId = pendingParents.get(cursor);
            cursor++;

            List<LogicalFile> currentNodes = logicalFileRepository.findByParentId(currentParentId);
            nodes.addAll(currentNodes);
            pendingParents.addAll(currentNodes.stream().filter(LogicalFile::isFolder).map(LogicalFile::getId).toList());
        }
    }

    private List<StorageNodeView> buildNodeViews(List<LogicalFile> nodes, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Map<String, PhysicalFile> physicalMap = toPhysicalMap(nodes.stream().filter(LogicalFile::isFile).toList());

        List<StorageNodeView> nodeViews = new ArrayList<>();
        for (LogicalFile node : nodes) {
            if (!matchKeyword(node.getFileName(), normalizedKeyword)) {
                continue;
            }
            nodeViews.add(toNodeView(node, physicalMap));
        }
        return nodeViews;
    }

    private Map<String, PhysicalFile> toPhysicalMap(List<LogicalFile> files) {
        Set<String> physicalIds = files.stream().map(LogicalFile::getPhysicalFileId).collect(Collectors.toSet());
        if (physicalIds.isEmpty()) {
            return Map.of();
        }
        return physicalFileRepository.findAllById(physicalIds).stream()
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
            case SIZE -> Comparator.comparingLong(this::sortableSize).thenComparing(node -> normalizeName(node.name()))
                    .thenComparing(StorageNodeView::id);
            case TYPE -> Comparator.comparingInt(this::typeRank).thenComparing(node -> normalizeName(node.name()))
                    .thenComparing(StorageNodeView::id);
            case UPDATED_AT ->
                    Comparator.comparing(this::sortableUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(node -> normalizeName(node.name())).thenComparing(StorageNodeView::id);
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

    /**
     * Converts a logical file entity to its storage node view representation.
     */
    public StorageNodeView toFileView(LogicalFile file) {
        assertFileNode(file);
        return toNodeView(file, toPhysicalMap(List.of(file)));
    }

    /**
     * Converts a logical node and its optional physical file map entry into a storage node view.
     */
    public StorageNodeView toNodeView(LogicalFile node, Map<String, PhysicalFile> physicalMap) {
        if (node == null) {
            throw new BizException(BaseError.NOT_FOUND);
        }
        PhysicalFile pf = node.isFile() ? physicalMap.get(node.getPhysicalFileId()) : null;
        String contentType = node.isFile() ? resolveContentType(node.getFileName(),
                pf != null ? pf.getContentType() : null) : null;
        return new StorageNodeView(node.getId(), node.getNodeType().name(), node.getFileName(), node.getParentId(),
                toContractOwnerType(node.getOwnerType()), node.getOwnerId(),
                node.isFile() ? (pf != null ? pf.getFileSize() : 0L) : null, contentType, node.getCreatedAt(),
                node.getUpdatedAt());
    }

    /**
     * Resolves the MIME content type for a file based on its extension if the provided
     * content type is null or blank.
     */
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

    private void assertFileNode(LogicalFile file) {
        if (file == null || !file.isFile()) {
            throw new BizException(BaseError.NOT_FOUND);
        }
    }
}

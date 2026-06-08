package com.corwin.system.file.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.SignUtil;
import com.corwin.framework.util.StrUtil;
import com.corwin.system.file.application.command.UploadFileCommand;
import com.corwin.system.file.published.FilePurpose;
import com.corwin.system.file.published.InternalFileType;
import com.corwin.system.file.published.OwnerType;
import com.corwin.system.file.application.port.FileCommandPort;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.file.domain.model.LogicalFile;
import com.corwin.system.file.domain.model.LogicalFolder;
import com.corwin.system.file.domain.model.PhysicalFile;
import com.corwin.system.file.domain.repo.LogicalFileRepository;
import com.corwin.system.file.domain.repo.LogicalFolderRepository;
import com.corwin.system.file.domain.repo.PhysicalFileRepository;
import com.corwin.system.file.infrastructure.storage.LocalStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Corwin 2026/2/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService implements FileCommandPort {

    private final LogicalFileRepository logicalFileRepository;
    private final LogicalFolderRepository logicalFolderRepository;
    private final PhysicalFileRepository physicalFileRepository;
    private final LocalStorageProvider storageProvider;

    @Override
    @Transactional
    public String uploadFile(UploadFileCommand cmd, InputStream inputStream, FilePurpose purpose) {
        Path tempFile = null;
        String hash;
        long size;
        try {
            tempFile = Files.createTempFile(storageProvider.getTempPath(), "breezy-upload-", ".tmp");
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
            try (InputStream is = Files.newInputStream(tempFile)) {
                hash = SignUtil.sha256(is);
            }
            size = Files.size(tempFile);
        } catch (Exception e) {
            throw new BizException("文件处理失败", BaseError.SERVICE_ERROR);
        }

        try {
            OwnerType domainOwnerType = toDomainOwnerType(cmd.ownerType());
            String finalFileName = cmd.fileName();
            if (cmd.ownerType() == OwnerType.USER) {
                finalFileName = resolveFileNameConflict(domainOwnerType, cmd.ownerId(), cmd.parentId(),
                        cmd.fileName());
            }

            PhysicalFile physicalFile = handlePhysicalFile(hash, tempFile, size, cmd.contentType());
            LogicalFile logicalFile = new LogicalFile(domainOwnerType, cmd.ownerId(), cmd.parentId(), finalFileName,
                    physicalFile.getId(), purpose.name());
            logicalFileRepository.save(logicalFile);
            return logicalFile.getId();
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("无法删除临时文件: {}", tempFile);
                }
            }
        }
    }

    @Override
    @Transactional
    public String createInternalFile(String fileName, InternalFileType fileType, byte[] content, FilePurpose purpose) {
        BizAssert.notBlank(fileName, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(fileType, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(content, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(purpose, BaseError.MISSING_PARAMETER);

        String systemOwnerId = DefaultUser.SYSTEM.account();
        String parentId = resolveApplicationPurposeFolderId(purpose, systemOwnerId);
        String finalFileName = resolveFileNameConflict(OwnerType.APPLICATION,
                systemOwnerId, parentId, normalizeInternalFileName(fileName, fileType));

        String hash;
        try (InputStream hashStream = new ByteArrayInputStream(content)) {
            hash = SignUtil.sha256(hashStream);
        } catch (IOException e) {
            throw new BizException("内部文件摘要计算失败", BaseError.SERVICE_ERROR);
        }

        PhysicalFile physicalFile = handlePhysicalFile(hash, content.length, fileType.contentType(),
                (relativePath, physicalFileName) -> {
                    try (InputStream writeStream = new ByteArrayInputStream(content)) {
                        storageProvider.write(relativePath, physicalFileName, writeStream);
                    }
                });

        LogicalFile logicalFile = new LogicalFile(OwnerType.APPLICATION,
                systemOwnerId, parentId, finalFileName, physicalFile.getId(), purpose.name());
        logicalFileRepository.save(logicalFile);
        return logicalFile.getId();
    }

    @Override
    public byte[] readFileContent(String fileId) {
        BizAssert.notBlank(fileId, BaseError.MISSING_PARAMETER);

        LogicalFile logicalFile = logicalFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException("File not found: " + fileId, BaseError.NOT_FOUND));
        PhysicalFile physicalFile = physicalFileRepository.findById(logicalFile.getPhysicalFileId())
                .orElseThrow(() -> new BizException("Physical file not found", BaseError.NOT_FOUND));

        try (InputStream inputStream = storageProvider.read(physicalFile.getRelativePath(), physicalFile.getFileName())) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new BizException("Read file content failed", BaseError.SERVICE_ERROR);
        }
    }

    private String resolveFileNameConflict(OwnerType ownerType, String ownerId,
            String parentId, String originalName) {
        Optional<LogicalFile> existing = logicalFileRepository.findByOwnerTypeAndOwnerIdAndParentIdAndFileName(
                ownerType, ownerId, parentId, originalName);
        if (existing.isEmpty()) {
            return originalName;
        }

        String baseName = originalName;
        String extension = "";
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            baseName = originalName.substring(0, lastDotIndex);
            extension = originalName.substring(lastDotIndex);
        }

        int count = 1;
        while (true) {
            String newName = String.format("%s (%d)%s", baseName, count, extension);
            if (logicalFileRepository.findByOwnerTypeAndOwnerIdAndParentIdAndFileName(ownerType, ownerId, parentId,
                    newName).isEmpty()) {
                return newName;
            }
            count++;
        }
    }

    private String normalizeInternalFileName(String fileName, InternalFileType fileType) {
        String normalized = StrUtil.trimToNull(fileName);
        BizAssert.notBlank(normalized, BaseError.MISSING_PARAMETER);
        String extension = fileType.defaultExtension();
        if (!normalized.toLowerCase().endsWith(extension.toLowerCase())) {
            normalized = normalized + extension;
        }
        return normalized;
    }

    private String resolveApplicationPurposeFolderId(FilePurpose purpose, String systemOwnerId) {
        Optional<LogicalFolder> existingFolder = logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentIdAndFolderName(
                OwnerType.APPLICATION, systemOwnerId, null, purpose.name());
        if (existingFolder.isPresent()) {
            return existingFolder.get().getId();
        }

        try {
            return createFolder(OwnerType.APPLICATION, systemOwnerId, null, purpose.name());
        } catch (BizException | DataIntegrityViolationException e) {
            return logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentIdAndFolderName(
                            OwnerType.APPLICATION, systemOwnerId, null,
                            purpose.name())
                    .map(LogicalFolder::getId)
                    .orElseThrow(() -> new BizException("系统用途目录创建失败", BaseError.SERVICE_ERROR));
        }
    }

    private PhysicalFile handlePhysicalFile(String hash, Path tempFile, long size, String contentType) {
        return handlePhysicalFile(hash, size, contentType, (relativePath, physicalFileName) -> {
            try (InputStream is = Files.newInputStream(tempFile)) {
                storageProvider.write(relativePath, physicalFileName, is);
            }
        });
    }

    private PhysicalFile handlePhysicalFile(String hash, long size, String contentType,
            PhysicalFileContentWriter contentWriter) {
        return physicalFileRepository.findByHash(hash).map(existing -> {
            physicalFileRepository.incrementRefCount(existing.getId());
            return existing;
        }).orElseGet(() -> {
            String relativePath = storageProvider.generateRelativePath(hash);
            try {
                contentWriter.write(relativePath, hash);
            } catch (IOException e) {
                throw new BizException("物理文件写入失败", BaseError.SERVICE_ERROR);
            }

            PhysicalFile pf = new PhysicalFile(hash, relativePath, hash, size, contentType);
            try {
                return physicalFileRepository.save(pf);
            } catch (DataIntegrityViolationException e) {
                log.info("检测到并发上传冲突，回退到增加引用计数模式: {}", hash);
                PhysicalFile existing = physicalFileRepository.findByHash(hash)
                        .orElseThrow(() -> new IllegalStateException("并发一致性异常"));
                physicalFileRepository.incrementRefCount(existing.getId());
                return existing;
            }
        });
    }

    @FunctionalInterface
    private interface PhysicalFileContentWriter {
        void write(String relativePath, String physicalFileName) throws IOException;
    }

    @Override
    @Transactional
    public void deleteFile(String fileId) {
        deleteFile(fileId, null, null);
    }

    @Override
    @Transactional
    public void deleteFile(String fileId, OwnerType expectedOwnerType, String expectedOwnerId) {
        logicalFileRepository.findById(fileId).ifPresent(file -> {
            assertFileOwner(file, toDomainOwnerType(expectedOwnerType), expectedOwnerId);
            deleteLogicalFile(file);
        });
    }

    private void handlePhysicalDecrement(String physicalId) {
        physicalFileRepository.decrementRefCount(physicalId);
        physicalFileRepository.findById(physicalId).ifPresent(pf -> {
            if (pf.canDelete()) {
                try {
                    storageProvider.delete(pf.getRelativePath(), pf.getFileName());
                    physicalFileRepository.delete(pf);
                    log.info("物理文件已由于引用计数归零而被删除: {}", pf.getHash());
                } catch (IOException e) {
                    log.error("物理文件清理失败: {}", pf.getHash(), e);
                }
            }
        });
    }

    @Transactional
    public String copyFile(String fileId, String targetParentId) {
        LogicalFile source = logicalFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));

        String normalizedTargetParentId = StrUtil.trimToNull(targetParentId);
        validateTargetFolder(source.getOwnerType(), source.getOwnerId(), normalizedTargetParentId);

        String finalName = resolveFileNameConflict(source.getOwnerType(), source.getOwnerId(), normalizedTargetParentId,
                source.getFileName());

        LogicalFile copy = new LogicalFile(source.getOwnerType(), source.getOwnerId(), normalizedTargetParentId,
                finalName, source.getPhysicalFileId(), source.getPurpose());
        logicalFileRepository.save(copy);
        physicalFileRepository.incrementRefCount(source.getPhysicalFileId());

        return copy.getId();
    }

    @Override
    @Transactional
    public String createFolder(OwnerType ownerType, String ownerId, String parentId, String name) {
        String normalizedParentId = StrUtil.trimToNull(parentId);
        String normalizedName = StrUtil.trimToNull(name);
        BizAssert.notBlank(normalizedName, BaseError.MISSING_PARAMETER);

        OwnerType domainOwnerType = toDomainOwnerType(ownerType);
        validateTargetFolder(domainOwnerType, ownerId, normalizedParentId);

        logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentIdAndFolderName(domainOwnerType, ownerId,
                normalizedParentId, normalizedName).ifPresent(existing -> {
            throw new BizException("同名目录已存在", BaseError.CONFLICT);
        });

        LogicalFolder folder = new LogicalFolder(domainOwnerType, ownerId, normalizedParentId, normalizedName);
        return logicalFolderRepository.save(folder).getId();
    }

    @Transactional
    public void renameFolder(String folderId, String newName) {
        renameFolder(folderId, newName, null, null);
    }

    @Override
    @Transactional
    public void renameFolder(String folderId, String newName, OwnerType expectedOwnerType, String expectedOwnerId) {
        String normalizedNewName = StrUtil.trimToNull(newName);
        BizAssert.notBlank(normalizedNewName, BaseError.MISSING_PARAMETER);

        LogicalFolder folder = logicalFolderRepository.findById(folderId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        assertFolderOwner(folder, toDomainOwnerType(expectedOwnerType), expectedOwnerId);

        if (folder.getFolderName().equals(normalizedNewName)) {
            return;
        }

        logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentIdAndFolderName(folder.getOwnerType(),
                folder.getOwnerId(), folder.getParentId(), normalizedNewName).ifPresent(existing -> {
            throw new BizException("同名目录已存在", BaseError.CONFLICT);
        });

        folder.rename(normalizedNewName);
        logicalFolderRepository.save(folder);
    }

    @Transactional
    public void renameFile(String fileId, String newName) {
        renameFile(fileId, newName, null, null);
    }

    @Override
    @Transactional
    public void renameFile(String fileId, String newName, OwnerType expectedOwnerType, String expectedOwnerId) {
        String normalizedNewName = StrUtil.trimToNull(newName);
        BizAssert.notBlank(normalizedNewName, BaseError.MISSING_PARAMETER);

        LogicalFile file = logicalFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        assertFileOwner(file, toDomainOwnerType(expectedOwnerType), expectedOwnerId);

        if (file.getFileName().equals(normalizedNewName)) {
            return;
        }

        String finalName = normalizedNewName;
        if (file.getOwnerType() == OwnerType.USER) {
            finalName = resolveFileNameConflict(file.getOwnerType(), file.getOwnerId(), file.getParentId(),
                    normalizedNewName);
        }

        file.rename(finalName);
        logicalFileRepository.save(file);
    }

    @Transactional
    public void deleteFolder(String folderId, boolean recursive) {
        deleteFolder(folderId, recursive, null, null);
    }

    @Override
    @Transactional
    public void deleteFolder(String folderId, boolean recursive, OwnerType expectedOwnerType, String expectedOwnerId) {
        LogicalFolder folder = logicalFolderRepository.findById(folderId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        assertFolderOwner(folder, toDomainOwnerType(expectedOwnerType), expectedOwnerId);

        if (!recursive) {
            BizAssert.state(logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentId(folder.getOwnerType(),
                    folder.getOwnerId(), folderId).isEmpty(), BaseError.SERVICE_ERROR);
            BizAssert.state(logicalFileRepository.findByOwnerTypeAndOwnerIdAndParentId(folder.getOwnerType(),
                    folder.getOwnerId(), folderId).isEmpty(), BaseError.SERVICE_ERROR);
            logicalFolderRepository.deleteById(folderId);
            return;
        }

        List<String> subtreeFolderIds = collectSubtreeFolderIds(folder);
        for (String currentFolderId : subtreeFolderIds) {
            List<LogicalFile> filesInFolder = logicalFileRepository.findByOwnerTypeAndOwnerIdAndParentId(
                    folder.getOwnerType(), folder.getOwnerId(), currentFolderId);
            for (LogicalFile logicalFile : filesInFolder) {
                deleteLogicalFile(logicalFile);
            }
        }

        for (int i = subtreeFolderIds.size() - 1; i >= 0; i--) {
            logicalFolderRepository.deleteById(subtreeFolderIds.get(i));
        }
    }

    @Override
    @Transactional
    public void moveFile(String fileId, String targetParentId, OwnerType expectedOwnerType, String expectedOwnerId) {
        LogicalFile file = logicalFileRepository.findById(fileId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        assertFileOwner(file, toDomainOwnerType(expectedOwnerType), expectedOwnerId);

        String normalizedTargetParentId = StrUtil.trimToNull(targetParentId);
        if (Objects.equals(file.getParentId(), normalizedTargetParentId)) {
            return;
        }

        validateTargetFolder(file.getOwnerType(), file.getOwnerId(), normalizedTargetParentId);

        String finalName = file.getFileName();
        if (file.getOwnerType() == OwnerType.USER) {
            finalName = resolveFileNameConflict(file.getOwnerType(), file.getOwnerId(), normalizedTargetParentId,
                    file.getFileName());
        }

        if (!finalName.equals(file.getFileName())) {
            file.rename(finalName);
        }
        file.move(normalizedTargetParentId);
        logicalFileRepository.save(file);
    }

    @Override
    @Transactional
    public void moveFolder(String folderId, String targetParentId, OwnerType expectedOwnerType,
            String expectedOwnerId) {
        LogicalFolder folder = logicalFolderRepository.findById(folderId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        assertFolderOwner(folder, toDomainOwnerType(expectedOwnerType), expectedOwnerId);

        String normalizedTargetParentId = StrUtil.trimToNull(targetParentId);
        if (Objects.equals(folder.getParentId(), normalizedTargetParentId)) {
            return;
        }
        if (Objects.equals(folderId, normalizedTargetParentId)) {
            throw new BizException("目录不能移动到自身", BaseError.ILLEGAL_ARGUMENT);
        }

        if (normalizedTargetParentId != null) {
            LogicalFolder targetFolder = validateTargetFolder(folder.getOwnerType(), folder.getOwnerId(),
                    normalizedTargetParentId);
            BizAssert.notNull(targetFolder, BaseError.NOT_FOUND);
            if (isAncestor(folder.getId(), targetFolder.getId())) {
                throw new BizException("目录不能移动到自己的子目录", BaseError.ILLEGAL_ARGUMENT);
            }
        }

        logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentIdAndFolderName(folder.getOwnerType(),
                folder.getOwnerId(), normalizedTargetParentId, folder.getFolderName()).ifPresent(existing -> {
            if (!existing.getId().equals(folderId)) {
                throw new BizException("目标目录存在同名文件夹", BaseError.CONFLICT);
            }
        });

        folder.move(normalizedTargetParentId);
        logicalFolderRepository.save(folder);
    }

    private void deleteLogicalFile(LogicalFile file) {
        String physicalId = file.getPhysicalFileId();
        logicalFileRepository.delete(file);
        handlePhysicalDecrement(physicalId);
    }

    private List<String> collectSubtreeFolderIds(LogicalFolder rootFolder) {
        List<String> folderIds = new ArrayList<>();
        List<String> pendingFolderIds = new ArrayList<>();
        pendingFolderIds.add(rootFolder.getId());
        int cursor = 0;
        while (cursor < pendingFolderIds.size()) {
            String currentFolderId = pendingFolderIds.get(cursor);
            cursor++;
            folderIds.add(currentFolderId);

            List<LogicalFolder> children = logicalFolderRepository.findByOwnerTypeAndOwnerIdAndParentId(
                    rootFolder.getOwnerType(), rootFolder.getOwnerId(), currentFolderId);
            pendingFolderIds.addAll(children.stream().map(LogicalFolder::getId).toList());
        }
        return folderIds;
    }

    private boolean isAncestor(String ancestorFolderId, String childFolderId) {
        String currentId = childFolderId;
        while (currentId != null) {
            if (ancestorFolderId.equals(currentId)) {
                return true;
            }
            LogicalFolder current = logicalFolderRepository.findById(currentId).orElse(null);
            if (current == null) {
                return false;
            }
            currentId = current.getParentId();
        }
        return false;
    }

    private LogicalFolder validateTargetFolder(OwnerType ownerType, String ownerId,
            String targetParentId) {
        if (targetParentId == null) {
            return null;
        }
        LogicalFolder targetFolder = logicalFolderRepository.findById(targetParentId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        boolean sameOwner = targetFolder.getOwnerType() == ownerType && Objects.equals(targetFolder.getOwnerId(),
                ownerId);
        if (!sameOwner) {
            throw new BizException("目标目录不属于当前所有者", BaseError.FORBIDDEN);
        }
        return targetFolder;
    }

    private void assertFileOwner(LogicalFile file, OwnerType expectedOwnerType,
            String expectedOwnerId) {
        if (expectedOwnerType == null || StrUtil.isBlank(expectedOwnerId)) {
            return;
        }
        boolean sameOwner = file.getOwnerType() == expectedOwnerType && Objects.equals(file.getOwnerId(),
                expectedOwnerId);
        if (!sameOwner) {
            throw new BizException("无权操作该文件", BaseError.FORBIDDEN);
        }
    }

    private void assertFolderOwner(LogicalFolder folder, OwnerType expectedOwnerType,
            String expectedOwnerId) {
        if (expectedOwnerType == null || StrUtil.isBlank(expectedOwnerId)) {
            return;
        }
        boolean sameOwner = folder.getOwnerType() == expectedOwnerType && Objects.equals(folder.getOwnerId(),
                expectedOwnerId);
        if (!sameOwner) {
            throw new BizException("无权操作该目录", BaseError.FORBIDDEN);
        }
    }

    private OwnerType toDomainOwnerType(OwnerType ownerType) {
        if (ownerType == null) {
            return null;
        }
        return OwnerType.valueOf(ownerType.name());
    }
}

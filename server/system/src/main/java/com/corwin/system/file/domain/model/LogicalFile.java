package com.corwin.system.file.domain.model;

import com.corwin.framework.util.HighDate;
import com.corwin.system.file.published.OwnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 逻辑文件节点。
 * 同时表示逻辑文件和逻辑目录。
 *
 * @author Corwin 2026/2/23
 */
@Getter
@Entity
@Table(name = "sys_storage_file", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"owner_type", "owner_id", "parent_id", "node_type", "file_name"})
})
public class LogicalFile {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 32)
    private OwnerType ownerType;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 16)
    private LogicalNodeType nodeType;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "physical_file_id", length = 36)
    private String physicalFileId;

    @Column(name = "purpose", length = 64)
    private String purpose;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LogicalFile() {
    }

    private LogicalFile(OwnerType ownerType, String ownerId, String parentId, LogicalNodeType nodeType, String fileName,
            String physicalFileId, String purpose) {
        this.id = UUID.randomUUID().toString();
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType required");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId required");
        this.parentId = parentId;
        this.nodeType = Objects.requireNonNull(nodeType, "nodeType required");
        this.fileName = Objects.requireNonNull(fileName, "fileName required");
        this.physicalFileId = physicalFileId;
        this.purpose = purpose;
        this.createdAt = HighDate.mockDateTime();
        this.updatedAt = this.createdAt;
    }

    public static LogicalFile file(OwnerType ownerType, String ownerId, String parentId, String fileName,
            String physicalFileId, String purpose) {
        return new LogicalFile(ownerType, ownerId, parentId, LogicalNodeType.FILE, fileName,
                Objects.requireNonNull(physicalFileId, "physicalFileId required"),
                Objects.requireNonNull(purpose, "purpose required"));
    }

    public static LogicalFile folder(OwnerType ownerType, String ownerId, String parentId, String fileName) {
        return new LogicalFile(ownerType, ownerId, parentId, LogicalNodeType.FOLDER, fileName, null, null);
    }

    public boolean isFile() {
        return nodeType == LogicalNodeType.FILE;
    }

    public boolean isFolder() {
        return nodeType == LogicalNodeType.FOLDER;
    }

    public void rename(String newName) {
        this.fileName = Objects.requireNonNull(newName, "new name required");
        this.updatedAt = HighDate.mockDateTime();
    }

    public void move(String newParentId) {
        this.parentId = newParentId;
        this.updatedAt = HighDate.mockDateTime();
    }

    public void replacePhysicalFile(String newPhysicalFileId) {
        if (!isFile()) {
            throw new IllegalStateException("folder node has no physical file");
        }
        this.physicalFileId = Objects.requireNonNull(newPhysicalFileId, "newPhysicalFileId required");
        this.updatedAt = HighDate.mockDateTime();
    }
}

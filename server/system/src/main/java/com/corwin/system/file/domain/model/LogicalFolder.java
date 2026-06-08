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
 * 逻辑文件夹。
 * 支持树形结构，基于 Owner 隔离。
 *
 * @author Corwin 2026/2/23
 */
@Getter
@Entity
@Table(name = "sys_storage_folder", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"owner_type", "owner_id", "parent_id", "folder_name"})
})
public class LogicalFolder {

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

    @Column(name = "folder_name", nullable = false, length = 256)
    private String folderName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected LogicalFolder() {
    }

    public LogicalFolder(OwnerType ownerType, String ownerId, String parentId, String folderName) {
        this.id = UUID.randomUUID().toString();
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType required");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId required");
        this.parentId = parentId;
        this.folderName = Objects.requireNonNull(folderName, "folderName required");
        this.createdAt = HighDate.mockDateTime();
        this.updatedAt = this.createdAt;
    }

    public void rename(String newName) {
        this.folderName = Objects.requireNonNull(newName, "new name required");
        this.updatedAt = HighDate.mockDateTime();
    }

    public void move(String newParentId) {
        this.parentId = newParentId;
        this.updatedAt = HighDate.mockDateTime();
    }
}

package com.corwin.system.file.domain.model;

import com.corwin.framework.util.HighDate;
import com.corwin.system.file.published.OwnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 逻辑文件。
 * 业务可见的文件实体，引用物理文件。
 *
 * @author Corwin 2026/2/23
 */
@Getter
@Entity
@Table(name = "sys_storage_file")
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

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "physical_file_id", nullable = false, length = 36)
    private String physicalFileId;

    @Column(name = "purpose", nullable = false, length = 64)
    private String purpose;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LogicalFile() {
    }

    public LogicalFile(OwnerType ownerType, String ownerId, String parentId, String fileName, String physicalFileId, String purpose) {
        this.id = UUID.randomUUID().toString();
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType required");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId required");
        this.parentId = parentId;
        this.fileName = Objects.requireNonNull(fileName, "fileName required");
        this.physicalFileId = Objects.requireNonNull(physicalFileId, "physicalFileId required");
        this.purpose = Objects.requireNonNull(purpose, "purpose required");
        this.createdAt = HighDate.mockDateTime();
        this.updatedAt = this.createdAt;
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
        this.physicalFileId = Objects.requireNonNull(newPhysicalFileId, "newPhysicalFileId required");
        this.updatedAt = HighDate.mockDateTime();
    }
}

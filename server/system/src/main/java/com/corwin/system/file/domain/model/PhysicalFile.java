package com.corwin.system.file.domain.model;

import com.corwin.framework.util.HighDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Physical file record representing a real file on disk.
 * <p>Files are de-duplicated by their SHA-256 hash. Multiple logical files may
 * reference the same physical file. The reference count tracks how many logical
 * references exist.</p>
 *
 * @author Corwin 2026/2/23
 */
@Getter
@Entity
@Table(name = "sys_storage_physical_file")
public class PhysicalFile {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "hash", nullable = false, unique = true, length = 128)
    private String hash;

    @Column(name = "relative_path", nullable = false, length = 256)
    private String relativePath;

    @Column(name = "file_name", nullable = false, length = 128)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "ref_count", nullable = false)
    private Integer refCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PhysicalFile() {
    }

    public PhysicalFile(String hash, String relativePath, String fileName, Long fileSize, String contentType) {
        this.id = UUID.randomUUID().toString();
        this.hash = Objects.requireNonNull(hash, "hash required");
        this.relativePath = Objects.requireNonNull(relativePath, "path required");
        this.fileName = Objects.requireNonNull(fileName, "filename required");
        this.fileSize = Objects.requireNonNull(fileSize, "size required");
        this.contentType = contentType;
        this.refCount = 1;
        this.createdAt = HighDate.mockDateTime();
    }

    /**
     * Increments the reference count for this physical file.
     * Called when a new logical file references this physical file.
     */
    public void incrementRef() {
        this.refCount++;
    }

    /**
     * Decrements the reference count for this physical file.
     * When the count reaches zero, the file becomes eligible for deletion.
     */
    public void decrementRef() {
        if (this.refCount > 0) {
            this.refCount--;
        }
    }

    /**
     * Returns {@code true} if this physical file has no remaining references
     * and can be safely deleted from disk.
     */
    public boolean canDelete() {
        return this.refCount <= 0;
    }
}

package com.corwin.system.file.interfaces.web.res;

import com.corwin.system.file.published.OwnerType;
import java.time.LocalDateTime;

/**
 * Response DTO containing detailed information about a logical file along with its underlying
 * physical file storage metadata.
 *
 * @param logicalFileId the logical file ID
 * @param logicalFileName the logical file name
 * @param logicalOwnerType the owner type of the logical file
 * @param logicalOwnerId the owner ID of the logical file
 * @param logicalParentId the parent folder ID of the logical file
 * @param physicalFileId the physical file ID
 * @param hash the SHA-256 hash of the physical file content
 * @param relativePath the relative storage path of the physical file
 * @param absolutePath the absolute filesystem path of the physical file
 * @param fileName the physical file name on disk
 * @param fileSize the physical file size in bytes
 * @param contentType the MIME content type
 * @param refCount the reference count of the physical file
 * @param physicalCreatedAt the creation timestamp of the physical file record
 * @author Corwin 2026/2/24
 */
public record PhysicalFileDetailRes(
    String logicalFileId,
    String logicalFileName,
    OwnerType logicalOwnerType,
    String logicalOwnerId,
    String logicalParentId,
    String physicalFileId,
    String hash,
    String relativePath,
    String absolutePath,
    String fileName,
    Long fileSize,
    String contentType,
    Integer refCount,
    LocalDateTime physicalCreatedAt) {}

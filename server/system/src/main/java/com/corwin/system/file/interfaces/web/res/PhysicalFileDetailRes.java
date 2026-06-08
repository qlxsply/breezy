package com.corwin.system.file.interfaces.web.res;

import com.corwin.system.file.published.OwnerType;

import java.time.LocalDateTime;

/**
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
        LocalDateTime physicalCreatedAt
) {
}

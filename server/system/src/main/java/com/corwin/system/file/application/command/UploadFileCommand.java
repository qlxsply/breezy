package com.corwin.system.file.application.command;

import com.corwin.system.file.published.OwnerType;

/**
 * @author Corwin 2026/2/23
 */
public record UploadFileCommand(
        OwnerType ownerType,
        String ownerId,
        String parentId,
        String fileName,
        String contentType
) {
}

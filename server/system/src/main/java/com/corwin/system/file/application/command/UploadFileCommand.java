package com.corwin.system.file.application.command;

import com.corwin.system.file.published.OwnerType;

/**
 * Command for uploading a file, specifying its ownership, parent folder, and metadata.
 *
 * @param ownerType the type of owner (USER or APPLICATION)
 * @param ownerId the identifier of the owner
 * @param parentId the parent folder ID (nullable for root)
 * @param fileName the original file name
 * @param contentType the MIME content type of the file
 * @author Corwin 2026/2/23
 */
public record UploadFileCommand(
    OwnerType ownerType, String ownerId, String parentId, String fileName, String contentType) {}

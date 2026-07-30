package com.corwin.system.file.interfaces.web.req;

import com.corwin.system.file.published.OwnerType;

/**
 * Request DTO for creating a new folder in the file storage tree.
 *
 * @param ownerType the type of owner
 * @param ownerId   the identifier of the owner
 * @param parentId  the parent folder ID (nullable for root)
 * @param name      the folder name
 * @author Corwin 2026/2/23
 */
public record CreateFolderReq(
        OwnerType ownerType,
        String ownerId,
        String parentId,
        String name
) {
}

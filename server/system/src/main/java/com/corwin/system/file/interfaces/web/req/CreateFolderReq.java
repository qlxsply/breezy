package com.corwin.system.file.interfaces.web.req;

import com.corwin.system.file.published.OwnerType;

/**
 * @author Corwin 2026/2/23
 */
public record CreateFolderReq(
        OwnerType ownerType,
        String ownerId,
        String parentId,
        String name
) {
}

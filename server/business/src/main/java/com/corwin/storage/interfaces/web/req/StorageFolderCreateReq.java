package com.corwin.storage.interfaces.web.req;

/**
 * @author Corwin 2026/2/23
 */
public record StorageFolderCreateReq(
        String parentId,
        String name
) {
}

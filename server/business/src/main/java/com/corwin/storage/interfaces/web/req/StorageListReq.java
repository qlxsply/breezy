package com.corwin.storage.interfaces.web.req;

import com.corwin.system.file.application.command.StorageSortBy;
import com.corwin.system.file.application.command.StorageSortOrder;

/**
 * @author Corwin 2026/7/29
 */
public record StorageListReq(
    String parentId,
    String keyword,
    Boolean recursive,
    StorageSortBy sortBy,
    StorageSortOrder sortOrder) {}

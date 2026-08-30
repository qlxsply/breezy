package com.corwin.system.file.interfaces.web.req;

import com.corwin.system.file.application.command.StorageSortBy;
import com.corwin.system.file.application.command.StorageSortOrder;

/**
 * Request DTO for listing storage nodes with optional keyword filtering, recursive traversal, and
 * sorting parameters.
 *
 * @param parentId the parent folder ID (nullable for root)
 * @param keyword optional keyword to filter by file name
 * @param recursive whether to recursively include descendants
 * @param sortBy field to sort by
 * @param sortOrder sort direction
 * @author Corwin 2026/7/29
 */
public record StorageListReq(
    String parentId,
    String keyword,
    Boolean recursive,
    StorageSortBy sortBy,
    StorageSortOrder sortOrder) {}

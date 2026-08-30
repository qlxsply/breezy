package com.corwin.system.file.application.command;

import com.corwin.framework.util.StrUtil;

/**
 * Query command for listing storage nodes with optional filtering, keyword search, recursive
 * traversal, and sort configuration.
 *
 * @param parentId the parent folder ID (nullable for root)
 * @param keyword optional keyword to filter by file name
 * @param recursive whether to recursively include all descendants
 * @param sortBy field to sort by (defaults to NAME)
 * @param sortOrder sort direction (defaults to ASC)
 * @author Corwin 2026/2/24
 */
public record StorageQueryCommand(
    String parentId,
    String keyword,
    boolean recursive,
    StorageSortBy sortBy,
    StorageSortOrder sortOrder) {
  public StorageQueryCommand {
    parentId = StrUtil.trimToNull(parentId);
    keyword = StrUtil.trimToNull(keyword);
    sortBy = sortBy == null ? StorageSortBy.NAME : sortBy;
    sortOrder = sortOrder == null ? StorageSortOrder.ASC : sortOrder;
  }
}

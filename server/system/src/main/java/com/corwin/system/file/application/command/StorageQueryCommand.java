package com.corwin.system.file.application.command;

import com.corwin.framework.util.StrUtil;

/**
 * @author Corwin 2026/2/24
 */
public record StorageQueryCommand(
        String parentId,
        String keyword,
        boolean recursive,
        StorageSortBy sortBy,
        StorageSortOrder sortOrder
) {
    public StorageQueryCommand {
        parentId = StrUtil.trimToNull(parentId);
        keyword = StrUtil.trimToNull(keyword);
        sortBy = sortBy == null ? StorageSortBy.NAME : sortBy;
        sortOrder = sortOrder == null ? StorageSortOrder.ASC : sortOrder;
    }
}

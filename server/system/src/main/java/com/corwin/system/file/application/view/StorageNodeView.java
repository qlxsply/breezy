package com.corwin.system.file.application.view;

import com.corwin.system.file.published.OwnerType;

import java.time.LocalDateTime;

/**
 * 存储节点视图（文件或文件夹）。
 *
 * @author Corwin 2026/2/23
 */
public record StorageNodeView(
        String id,
        String type,
        // "FOLDER" 或 "FILE"
        String name,
        String parentId,
        OwnerType ownerType,
        String ownerId,
        Long size,
        // 仅文件有值
        String contentType,
        // 仅文件有值
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

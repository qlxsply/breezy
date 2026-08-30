package com.corwin.storage.interfaces.web.res;

import java.time.LocalDateTime;

/**
 * @author Corwin 2026/2/23
 */
public record StorageItemRes(
    String id,
    String type, // "FOLDER" 或 "FILE"
    String name,
    String parentId,
    Long size,
    String contentType,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}

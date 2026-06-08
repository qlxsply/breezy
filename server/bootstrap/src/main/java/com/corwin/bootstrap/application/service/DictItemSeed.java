package com.corwin.bootstrap.application.service;

/**
 * @author Corwin 2026/4/16
 */
public record DictItemSeed(
        String id,
        String parentItemId,
        String itemCode,
        String itemLabel,
        String itemValue,
        int sortNo,
        boolean enabled,
        boolean defaultItem,
        String tagColor,
        String tagType,
        String extraJson,
        String description
) {
}

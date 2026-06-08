package com.corwin.system.dict.application.command;

/**
 * @author Corwin 2026/3/15
 */
public record CreateDictItemCommand(
        String parentItemId,
        String itemCode,
        String itemLabel,
        String itemValue,
        Integer sortNo,
        boolean enabled,
        boolean defaultItem,
        String tagColor,
        String tagType,
        String extraJson,
        String description
) {
}

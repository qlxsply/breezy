package com.corwin.system.dict.application.command;

/**
 * Command representing a single dictionary item within a draft-based save operation.
 *
 * @author Corwin 2026/5/18
 */
public record SaveDictTypeItemCommand(
        String id,
        String clientKey,
        String parentClientKey,
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

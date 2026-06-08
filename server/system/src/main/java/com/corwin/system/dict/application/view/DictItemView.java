package com.corwin.system.dict.application.view;

/**
 * @author Corwin 2026/3/15
 */
public record DictItemView(
        String id,
        String dictTypeId,
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

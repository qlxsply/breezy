package com.corwin.system.dict.interfaces.web.res;

/**
 * Response DTO for dictionary item data.
 *
 * @author Corwin 2026/3/15
 */
public record DictItemRes(
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

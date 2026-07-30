package com.corwin.system.dict.interfaces.web.req;

/**
 * Request DTO for creating a new dictionary item.
 *
 * @author Corwin 2026/3/15
 */
public record CreateDictItemReq(
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

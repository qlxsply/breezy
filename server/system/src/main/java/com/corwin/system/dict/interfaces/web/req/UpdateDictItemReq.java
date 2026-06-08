package com.corwin.system.dict.interfaces.web.req;

/**
 * @author Corwin 2026/3/15
 */
public record UpdateDictItemReq(
        String parentItemId,
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

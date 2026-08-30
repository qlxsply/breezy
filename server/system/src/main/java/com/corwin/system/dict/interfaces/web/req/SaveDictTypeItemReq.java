package com.corwin.system.dict.interfaces.web.req;

/**
 * Request DTO representing a single item within a dictionary type save/update payload.
 *
 * @author Corwin 2026/5/18
 */
public record SaveDictTypeItemReq(
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
    String description) {}

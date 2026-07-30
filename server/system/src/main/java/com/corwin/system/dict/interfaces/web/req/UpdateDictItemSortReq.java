package com.corwin.system.dict.interfaces.web.req;

import java.util.List;

/**
 * Request DTO for updating the sort order of dictionary items.
 *
 * @author Corwin 2026/3/15
 */
public record UpdateDictItemSortReq(List<String> itemIds) {
}

package com.corwin.system.config.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;

/**
 * Request DTO for paginated configuration listing.
 * Supports optional fuzzy search by config code and description.
 *
 * @author Corwin 2026/7/29
 */
public record ConfigPageReq(
        PageRuleRequest page,
        String codeLike,
        String descriptionLike
) {
}

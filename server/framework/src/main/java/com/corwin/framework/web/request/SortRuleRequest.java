package com.corwin.framework.web.request;

import com.corwin.framework.domain.page.SortSpec;

import java.util.List;

/**
 * Request DTO for sort parameters.
 *
 * @param orders the list of sort specifications
 * @author Corwin 2026/4/1
 */
public record SortRuleRequest(
        List<SortSpec> orders
) {
}

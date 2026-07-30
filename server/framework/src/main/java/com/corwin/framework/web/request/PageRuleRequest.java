package com.corwin.framework.web.request;

/**
 * Request DTO for pagination parameters.
 *
 * @param pageNo   the requested page number (1-based), may be null
 * @param pageSize the requested page size, may be null
 * @author Corwin 2026/4/1
 */
public record PageRuleRequest(
        Integer pageNo,
        Integer pageSize
) {
}

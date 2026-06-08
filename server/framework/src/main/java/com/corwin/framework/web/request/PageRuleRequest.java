package com.corwin.framework.web.request;

/**
 * @author Corwin 2026/4/1
 */
public record PageRuleRequest(
        Integer pageNo,
        Integer pageSize
) {
}

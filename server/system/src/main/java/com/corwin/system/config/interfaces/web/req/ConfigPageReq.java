package com.corwin.system.config.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

/**
 * @author Corwin 2026/7/31
 */
public record ConfigPageReq(
    @Valid PageRuleRequest page,
    @Size(max = 160) String keyword,
    @Size(max = 64) String module,
    @Size(max = 64) String group) {}

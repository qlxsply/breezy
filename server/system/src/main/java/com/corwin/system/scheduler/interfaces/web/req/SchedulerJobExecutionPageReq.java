package com.corwin.system.scheduler.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;

/**
 * Request DTO for paginated job execution history query.
 *
 * @author Corwin 2026/7/29
 */
public record SchedulerJobExecutionPageReq(PageRuleRequest page) {}

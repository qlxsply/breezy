package com.corwin.reminder.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;
import com.corwin.reminder.domain.model.ScheduleEventStatus;

/**
 * @author Corwin 2026/1/12
 */
public record SchedulePageReq(
    PageRuleRequest page, SortRuleRequest sort, ScheduleEventStatus status, String titleLike) {}

package com.corwin.reminder.interfaces.web.req;

import com.corwin.framework.web.request.PageRuleRequest;
import com.corwin.framework.web.request.SortRuleRequest;
import com.corwin.reminder.domain.model.TodoTaskStatus;

/**
 * @author Corwin 2026/1/12
 */
public record TodoPageReq(
    PageRuleRequest page, SortRuleRequest sort, TodoTaskStatus status, String contentLike) {}

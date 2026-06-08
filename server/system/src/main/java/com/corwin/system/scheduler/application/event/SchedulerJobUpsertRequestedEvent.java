package com.corwin.system.scheduler.application.event;

import com.corwin.system.scheduler.domain.model.DynamicJobSpec;

/**
 * 动态任务新增或更新请求事件。
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobUpsertRequestedEvent(
        DynamicJobSpec<?> spec
) {
}

package com.corwin.system.scheduler.application.event;

import com.corwin.system.scheduler.domain.model.DynamicJobSpec;

/**
 * Event fired when a job upsert (create or update) is requested.
 *
 * @author Corwin 2026/4/15
 */
public record SchedulerJobUpsertRequestedEvent(
        DynamicJobSpec<?> spec
) {
}

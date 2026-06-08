package com.corwin.system.scheduler.application.listener;

import com.corwin.system.scheduler.application.event.SchedulerJobCancelRequestedEvent;
import com.corwin.system.scheduler.application.event.SchedulerJobDeleteRequestedEvent;
import com.corwin.system.scheduler.application.event.SchedulerJobPauseRequestedEvent;
import com.corwin.system.scheduler.application.event.SchedulerJobResumeRequestedEvent;
import com.corwin.system.scheduler.application.event.SchedulerJobUpsertRequestedEvent;
import com.corwin.system.scheduler.application.service.SchedulerCommandAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 动态任务事件监听器。
 *
 * @author Corwin 2026/4/15
 */
@Component
@RequiredArgsConstructor
public class SchedulerJobRequestListener {

    private final SchedulerCommandAppService commandAppService;

    @EventListener
    public void onUpsert(SchedulerJobUpsertRequestedEvent event) {
        commandAppService.handleUpsert(event.spec());
    }

    @EventListener
    public void onPause(SchedulerJobPauseRequestedEvent event) {
        commandAppService.pauseRequested(event.jobId());
    }

    @EventListener
    public void onResume(SchedulerJobResumeRequestedEvent event) {
        commandAppService.resumeRequested(event.jobId());
    }

    @EventListener
    public void onCancel(SchedulerJobCancelRequestedEvent event) {
        commandAppService.cancelRequested(event.jobId());
    }

    @EventListener
    public void onDelete(SchedulerJobDeleteRequestedEvent event) {
        commandAppService.deleteRequested(event.jobId());
    }
}

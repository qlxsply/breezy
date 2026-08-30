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
 * Event listener that dispatches scheduler job request events to the command service.
 *
 * @author Corwin 2026/4/15
 */
@Component
@RequiredArgsConstructor
public class SchedulerJobRequestListener {

  private final SchedulerCommandAppService commandAppService;

  /** Handles job upsert (create/update) requests. */
  @EventListener
  public void onUpsert(SchedulerJobUpsertRequestedEvent event) {
    commandAppService.handleUpsert(event.spec());
  }

  /** Handles job pause requests. */
  @EventListener
  public void onPause(SchedulerJobPauseRequestedEvent event) {
    commandAppService.pauseRequested(event.jobId());
  }

  /** Handles job resume requests. */
  @EventListener
  public void onResume(SchedulerJobResumeRequestedEvent event) {
    commandAppService.resumeRequested(event.jobId());
  }

  /** Handles job cancel requests. */
  @EventListener
  public void onCancel(SchedulerJobCancelRequestedEvent event) {
    commandAppService.cancelRequested(event.jobId());
  }

  /** Handles job delete requests. */
  @EventListener
  public void onDelete(SchedulerJobDeleteRequestedEvent event) {
    commandAppService.deleteRequested(event.jobId());
  }
}

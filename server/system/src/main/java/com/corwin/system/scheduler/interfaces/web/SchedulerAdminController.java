package com.corwin.system.scheduler.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.scheduler.application.service.SchedulerCommandAppService;
import com.corwin.system.scheduler.application.service.SchedulerQueryAppService;
import com.corwin.system.scheduler.application.view.SchedulerJobDetailView;
import com.corwin.system.scheduler.application.view.SchedulerJobExecutionView;
import com.corwin.system.scheduler.application.view.SchedulerJobView;
import com.corwin.system.scheduler.interfaces.web.req.SchedulerJobExecutionPageReq;
import com.corwin.system.scheduler.interfaces.web.res.SchedulerJobDetailRes;
import com.corwin.system.scheduler.interfaces.web.res.SchedulerJobExecutionRes;
import com.corwin.system.scheduler.interfaces.web.res.SchedulerJobRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for scheduler job administration (list, detail, pause, resume, cancel, trigger, delete).
 *
 * @author Corwin 2026/4/15
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/admin/scheduler/jobs")
@RequiredArgsConstructor
public class SchedulerAdminController {

    private final SchedulerQueryAppService queryAppService;
    private final SchedulerCommandAppService commandAppService;

    /** Lists all non-deleted jobs with runtime status. */
    @GetMapping
    @Authorize(userType = UserType.ADMIN, permissions = {"scheduler.job.view"})
    public ApiResponse<List<SchedulerJobRes>> list() {
        return ApiResponse.ok(queryAppService.listJobs().stream().map(SchedulerAdminController::toRes).toList());
    }

    /** Returns detailed information for a single job. */
    @GetMapping("/{jobId}")
    @Authorize(userType = UserType.ADMIN, permissions = {"scheduler.job.view"})
    public ApiResponse<SchedulerJobDetailRes> detail(@PathVariable String jobId) {
        return ApiResponse.ok(toDetailRes(queryAppService.getJob(jobId)));
    }

    /** Paginated execution history for a specific job. */
    @PostMapping("/{jobId}/executions")
    @Authorize(userType = UserType.ADMIN, permissions = {"scheduler.job.view"})
    public ApiResponse<PageData<SchedulerJobExecutionRes>> executions(@PathVariable String jobId,
            @RequestBody SchedulerJobExecutionPageReq req) {
        PageData<SchedulerJobExecutionView> page = queryAppService.pageExecutions(jobId,
                PageSpecFactory.of(req.page(), null));
        return ApiResponse.ok(PageData.of(page.pageNo(), page.pageSize(), page.totalElements(),
                page.elements().stream().map(SchedulerAdminController::toExecutionRes).toList()));
    }

    /** Pauses a scheduled job. */
    @PostMapping("/{jobId}/pause")
    @Authorize(userType = UserType.ADMIN, permissions = {"scheduler.job.pause"})
    public ApiResponse<Boolean> pause(@PathVariable String jobId) {
        commandAppService.pauseRequested(jobId);
        return ApiResponse.ok(true);
    }

    /** Resumes a paused job. */
    @PostMapping("/{jobId}/resume")
    @Authorize(userType = UserType.ADMIN, permissions = {"scheduler.job.resume"})
    public ApiResponse<Boolean> resume(@PathVariable String jobId) {
        commandAppService.resumeRequested(jobId);
        return ApiResponse.ok(true);
    }

    /** Cancels a scheduled job. */
    @PostMapping("/{jobId}/cancel")
    @Authorize(userType = UserType.ADMIN, permissions = {"scheduler.job.cancel"})
    public ApiResponse<Boolean> cancel(@PathVariable String jobId) {
        commandAppService.cancelRequested(jobId);
        return ApiResponse.ok(true);
    }

    /** Triggers an immediate job execution. */
    @PostMapping("/{jobId}/trigger")
    @Authorize(userType = UserType.ADMIN, permissions = {"scheduler.job.trigger"})
    public ApiResponse<Boolean> trigger(@PathVariable String jobId) {
        commandAppService.triggerNow(jobId);
        return ApiResponse.ok(true);
    }

    /** Soft-deletes a job. */
    @DeleteMapping("/{jobId}")
    @Authorize(userType = UserType.ADMIN, permissions = {"scheduler.job.delete"})
    public ApiResponse<Boolean> delete(@PathVariable String jobId) {
        commandAppService.deleteRequested(jobId);
        return ApiResponse.ok(true);
    }

    private static SchedulerJobRes toRes(SchedulerJobView view) {
        return new SchedulerJobRes(view.jobId(), view.namespace(), view.name(), view.jobType(), view.handlerKey(),
                view.scheduleRuleType(), view.enabled(), view.allowConcurrent(), view.remark(), view.status(),
                view.nextFireTime(), view.lastFireTime(), view.lastSuccessTime(), view.lastFailureTime(),
                view.lastErrorMessage(), view.updatedAt());
    }

    private static SchedulerJobDetailRes toDetailRes(SchedulerJobDetailView view) {
        return new SchedulerJobDetailRes(view.jobId(), view.namespace(), view.name(), view.source(), view.jobType(),
                view.handlerKey(), view.scheduleRuleType(), view.payloadType(), view.enabled(), view.deleted(),
                view.allowConcurrent(), view.versionNo(), view.remark(), view.status(), view.nextFireTime(),
                view.lastFireTime(), view.lastSuccessTime(), view.lastFailureTime(), view.consecutiveFailures(),
                view.lastErrorMessage(), view.lastDurationMs(), view.currentExecutionId(), view.createdAt(),
                view.updatedAt());
    }

    private static SchedulerJobExecutionRes toExecutionRes(SchedulerJobExecutionView view) {
        return new SchedulerJobExecutionRes(view.executionId(), view.jobId(), view.scheduledTime(), view.startTime(),
                view.endTime(), view.result(), view.durationMs(), view.triggerType(), view.resultCode(),
                view.resultMessage(), view.errorMessage());
    }
}

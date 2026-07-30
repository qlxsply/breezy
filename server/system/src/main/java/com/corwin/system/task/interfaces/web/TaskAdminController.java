package com.corwin.system.task.interfaces.web;

import com.corwin.system.task.application.service.TaskAppService;
import com.corwin.system.task.application.view.TaskView;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.task.interfaces.web.req.UpdateTaskConfigReq;
import com.corwin.system.task.interfaces.web.res.TaskRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for task administration operations.
 * Provides endpoints for listing, configuring, publishing, stopping, and running tasks.
 *
 * @author Corwin 2026/3/30
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
public class TaskAdminController {

    private final TaskAppService taskAppService;

    /**
     * Retrieves a list of all non-removed tasks.
     *
     * @return the list of task responses
     */
    @GetMapping
    @Authorize(userType = UserType.ADMIN, permissions = {"task.view"})
    public ApiResponse<List<TaskRes>> list() {
        return ApiResponse.ok(taskAppService.listTasks().stream().map(TaskAdminController::toRes).toList());
    }

    /**
     * Updates the cron expression configuration for a task.
     *
     * @param code the task code
     * @param req  the request containing the new cron expression
     * @return true if the update succeeded
     */
    @PutMapping("/{code}/config")
    @Authorize(userType = UserType.ADMIN, permissions = {"task.edit"})
    public ApiResponse<Boolean> updateConfig(@PathVariable String code, @RequestBody UpdateTaskConfigReq req) {
        taskAppService.updateTaskConfig(code, req.cronExpr());
        return ApiResponse.ok(true);
    }

    /**
     * Publishes a task, activating its scheduled execution.
     *
     * @param code the task code
     * @return true if the publish succeeded
     */
    @PostMapping("/{code}/publish")
    @Authorize(userType = UserType.ADMIN, permissions = {"task.publish"})
    public ApiResponse<Boolean> publish(@PathVariable String code) {
        taskAppService.publishTask(code);
        return ApiResponse.ok(true);
    }

    /**
     * Stops a running task and disables its schedule.
     *
     * @param code the task code
     * @return true if the stop succeeded
     */
    @PostMapping("/{code}/stop")
    @Authorize(userType = UserType.ADMIN, permissions = {"task.publish"})
    public ApiResponse<Boolean> stop(@PathVariable String code) {
        taskAppService.stopTask(code);
        return ApiResponse.ok(true);
    }

    /**
     * Immediately executes a task once, regardless of its schedule status.
     *
     * @param code the task code
     * @return true if the execution was triggered
     */
    @PostMapping("/{code}/run")
    @Authorize(userType = UserType.ADMIN, permissions = {"task.run"})
    public ApiResponse<Boolean> run(@PathVariable String code) {
        taskAppService.runTask(code);
        return ApiResponse.ok(true);
    }

    private static TaskRes toRes(TaskView view) {
        return new TaskRes(view.code(), view.name(), view.description(), view.beanName(), view.methodName(),
                view.removed(), view.cronExpr(), view.status(), view.lastRunAt(), view.lastRunStatus(),
                view.lastErrorMessage(), view.updatedAt());
    }
}

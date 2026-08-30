package com.corwin.reminder.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.reminder.application.command.TodoCompleteCommand;
import com.corwin.reminder.application.command.TodoReorderCommand;
import com.corwin.reminder.application.command.TodoSaveCommand;
import com.corwin.reminder.application.service.TodoAppService;
import com.corwin.reminder.application.view.TodoDailyDetailView;
import com.corwin.reminder.application.view.TodoDailyStatsView;
import com.corwin.reminder.application.view.TodoView;
import com.corwin.reminder.interfaces.web.req.*;
import com.corwin.reminder.interfaces.web.res.TodoDailyDetailRes;
import com.corwin.reminder.interfaces.web.res.TodoDailyStatsRes;
import com.corwin.reminder.interfaces.web.res.TodoRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author Corwin 2026/1/12
 */
@ApiMeta(module = ApiModuleCode.REMINDER)
@Authorize(
    userType = UserType.USER,
    permissions = {"tdo.use"})
@RestController
@RequestMapping("/api/todo")
@RequiredArgsConstructor
public class TodoController {

  private final TodoAppService service;

  @PostMapping
  public ApiResponse<Long> create(@RequestBody TodoCreateReq req) {
    Long id =
        service.create(
            new TodoSaveCommand(
                req.content(),
                req.dueTime(),
                req.note(),
                req.contentAttachmentFileIds(),
                req.completionAttachmentFileIds()));
    return ApiResponse.ok(id);
  }

  @PutMapping("/{id}")
  public ApiResponse<Object> update(@PathVariable Long id, @RequestBody TodoUpdateReq req) {
    service.update(
        id,
        new TodoSaveCommand(
            req.content(),
            req.dueTime(),
            req.note(),
            req.contentAttachmentFileIds(),
            req.completionAttachmentFileIds()));
    return ApiResponse.ok();
  }

  @GetMapping("/{id}")
  public ApiResponse<TodoRes> get(@PathVariable Long id) {
    return ApiResponse.ok(toDto(service.get(id)));
  }

  @PostMapping("/page")
  public ApiResponse<PageResult<TodoRes>> page(@RequestBody TodoPageReq req) {
    var page =
        service.page(req.status(), req.contentLike(), PageSpecFactory.of(req.page(), req.sort()));
    return ApiResponse.ok(PageResult.of(page, TodoController::toDto));
  }

  @PostMapping("/reorder")
  public ApiResponse<Object> reorder(@RequestBody TodoReorderReq req) {
    List<TodoReorderCommand.GroupCommand> groups =
        req.groups() == null
            ? List.of()
            : req.groups().stream()
                .map(
                    group ->
                        new TodoReorderCommand.GroupCommand(group.status(), group.orderedIds()))
                .toList();
    service.reorder(new TodoReorderCommand(groups));
    return ApiResponse.ok();
  }

  @PostMapping("/stats/daily")
  public ApiResponse<List<TodoDailyStatsRes>> dailyStats(@RequestBody TodoDailyStatsReq req) {
    return ApiResponse.ok(
        service.dailyStats(req.startDate(), req.endDate()).stream()
            .map(TodoController::toDailyStatsRes)
            .toList());
  }

  @GetMapping("/stats/daily/{date}")
  public ApiResponse<TodoDailyDetailRes> dailyDetail(@PathVariable LocalDate date) {
    return ApiResponse.ok(toDailyDetailRes(service.dailyDetail(date)));
  }

  @PostMapping("/{id}/done")
  public ApiResponse<Object> done(@PathVariable Long id) {
    service.quickComplete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/complete")
  public ApiResponse<Object> complete(@PathVariable Long id, @RequestBody TodoCompleteReq req) {
    service.complete(
        id, new TodoCompleteCommand(req.completionNote(), req.completionAttachmentFileIds()));
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/complete/quick")
  public ApiResponse<Object> quickComplete(@PathVariable Long id) {
    service.quickComplete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/pause")
  public ApiResponse<Object> pause(@PathVariable Long id) {
    service.pause(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/resume")
  public ApiResponse<Object> resume(@PathVariable Long id) {
    service.resume(id);
    return ApiResponse.ok();
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Object> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok();
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<Object> cancel(@PathVariable Long id) {
    service.pause(id);
    return ApiResponse.ok();
  }

  private static TodoRes toDto(TodoView view) {
    return new TodoRes(
        view.id(),
        view.content(),
        view.dueTime(),
        view.status(),
        view.note(),
        view.contentAttachmentFileIds(),
        view.completionAttachmentFileIds(),
        view.sortNo(),
        view.completionNote(),
        view.completedAt(),
        view.createdAt());
  }

  private static TodoDailyStatsRes toDailyStatsRes(TodoDailyStatsView view) {
    return new TodoDailyStatsRes(view.date(), view.createdCount(), view.completedCount());
  }

  private static TodoDailyDetailRes toDailyDetailRes(TodoDailyDetailView view) {
    return new TodoDailyDetailRes(
        view.date(),
        view.createdItems().stream().map(TodoController::toDto).toList(),
        view.completedItems().stream().map(TodoController::toDto).toList());
  }
}

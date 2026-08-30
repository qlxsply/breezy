package com.corwin.reminder.application.service;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.reminder.application.command.TodoCompleteCommand;
import com.corwin.reminder.application.command.TodoReorderCommand;
import com.corwin.reminder.application.command.TodoSaveCommand;
import com.corwin.reminder.application.event.TodoTaskReminderChangedEvent;
import com.corwin.reminder.application.view.TodoDailyDetailView;
import com.corwin.reminder.application.view.TodoDailyStatsView;
import com.corwin.reminder.application.view.TodoView;
import com.corwin.reminder.domain.error.ReminderError;
import com.corwin.reminder.domain.model.TodoAttachment;
import com.corwin.reminder.domain.model.TodoTask;
import com.corwin.reminder.domain.model.TodoTaskStatus;
import com.corwin.reminder.domain.repo.TodoAttachmentRepository;
import com.corwin.reminder.domain.repo.TodoTaskPageQuery;
import com.corwin.reminder.domain.repo.TodoTaskRepository;
import com.corwin.system.file.application.port.FileCommandPort;
import com.corwin.system.user.config.SystemUserConfigSpecs;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Corwin 2026/1/12
 */
@Service
@RequiredArgsConstructor
public class TodoAppService {
  private static final int TODO_TEXT_MAX_LENGTH = 2000;
  private final TodoTaskRepository repo;
  private final TodoAttachmentRepository attachmentRepo;
  private final FileCommandPort fileService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public Long create(TodoSaveCommand command) {
    BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
    String content = normalizeContent(command.content());
    TodoTask task = TodoTask.create(content, command.dueTime());
    task.update(content, command.dueTime(), normalizeOptionalText(command.note()));
    task.assignOwner(currentUserId());
    repo.save(task);
    replaceAttachments(
        task, command.contentAttachmentFileIds(), command.completionAttachmentFileIds());
    persistAttachments(task);
    eventPublisher.publishEvent(
        TodoTaskReminderChangedEvent.updated(task.getId(), task.getRemindAt()));
    return task.getId();
  }

  @Transactional
  public void update(Long id, TodoSaveCommand command) {
    Objects.requireNonNull(id, "id required");
    BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
    TodoTask task = getTask(id);
    Set<String> before = new HashSet<>(task.allAttachmentFileIds());
    task.update(
        normalizeContent(command.content()),
        command.dueTime(),
        normalizeOptionalText(command.note()));
    repo.save(task);
    replaceAttachments(
        task, command.contentAttachmentFileIds(), command.completionAttachmentFileIds());
    persistAttachments(task);
    cleanupRemovedFiles(before, new HashSet<>(task.allAttachmentFileIds()));
    eventPublisher.publishEvent(
        TodoTaskReminderChangedEvent.updated(task.getId(), task.getRemindAt()));
  }

  public TodoView get(Long id) {
    return toView(getTask(id));
  }

  public PageData<TodoView> page(TodoTaskStatus status, String contentLike, PageSpec spec) {
    TodoTaskPageQuery query =
        new TodoTaskPageQuery(expandStatus(status), StrUtil.trimToNull(contentLike));
    PageData<TodoTask> page = withAttachments(repo.pageByQuery(query, PageSpecSorts.apply(spec)));
    return new PageData<>(
        page.pageNo(),
        page.pageSize(),
        page.numberOfElements(),
        page.totalPages(),
        page.totalElements(),
        page.elements().stream().map(this::toView).toList());
  }

  @Transactional
  public void reorder(TodoReorderCommand command) {
    BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
    BizAssert.notEmpty(command.groups(), BaseError.MISSING_PARAMETER);
    List<TodoReorderCommand.GroupCommand> groups =
        command.groups().stream().filter(Objects::nonNull).toList();
    BizAssert.notEmpty(groups, BaseError.MISSING_PARAMETER);
    LinkedHashSet<Long> ids = new LinkedHashSet<>();
    for (var group : groups) {
      BizAssert.notNull(group.status(), BaseError.MISSING_PARAMETER);
      BizAssert.notEmpty(group.orderedIds(), BaseError.MISSING_PARAMETER);
      for (Long id : group.orderedIds()) {
        BizAssert.notNull(id, BaseError.MISSING_PARAMETER);
        BizAssert.state(ids.add(id), BaseError.CONFLICT);
      }
    }
    List<TodoTask> tasks = withAttachments(repo.findByIdIn(List.copyOf(ids)));
    BizAssert.state(tasks.size() == ids.size(), ReminderError.TODO_NOT_FOUND);
    Map<Long, TodoTask> taskMap = new LinkedHashMap<>();
    tasks.forEach(task -> taskMap.put(task.getId(), task));
    List<TodoTask> updated = new ArrayList<>();
    for (var group : groups) {
      for (int i = 0; i < group.orderedIds().size(); i++) {
        TodoTask task = taskMap.get(group.orderedIds().get(i));
        if (task == null) throw new BizException(ReminderError.TODO_NOT_FOUND);
        applyStatus(task, group.status());
        task.applySortNo((long) i);
        updated.add(task);
      }
    }
    repo.saveAll(updated);
    updated.forEach(
        task ->
            eventPublisher.publishEvent(
                task.getStatus() == TodoTaskStatus.TODO
                    ? TodoTaskReminderChangedEvent.updated(task.getId(), task.getRemindAt())
                    : TodoTaskReminderChangedEvent.removed(task.getId())));
  }

  public List<TodoDailyStatsView> dailyStats(LocalDate startDate, LocalDate endDate) {
    BizAssert.notNull(startDate, BaseError.MISSING_PARAMETER);
    BizAssert.notNull(endDate, BaseError.MISSING_PARAMETER);
    BizAssert.state(!endDate.isBefore(startDate), BaseError.ILLEGAL_ARGUMENT);
    ZoneId zoneId = defaultZoneId();
    Instant start = startDate.atStartOfDay(zoneId).toInstant();
    Instant end = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
    Map<LocalDate, Integer> created =
        buildDailyCountMap(
            repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end), zoneId, true);
    Map<LocalDate, Integer> completed =
        buildDailyCountMap(
            repo.findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(start, end),
            zoneId,
            false);
    List<TodoDailyStatsView> result = new ArrayList<>();
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      result.add(
          new TodoDailyStatsView(
              date, created.getOrDefault(date, 0), completed.getOrDefault(date, 0)));
    }
    return result;
  }

  public TodoDailyDetailView dailyDetail(LocalDate date) {
    BizAssert.notNull(date, BaseError.MISSING_PARAMETER);
    ZoneId zoneId = defaultZoneId();
    Instant start = date.atStartOfDay(zoneId).toInstant();
    Instant end = date.plusDays(1).atStartOfDay(zoneId).toInstant();
    List<TodoView> created =
        withAttachments(repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(start, end))
            .stream()
            .sorted((a, b) -> compareInstantDesc(a.getCreatedAt(), b.getCreatedAt()))
            .map(this::toView)
            .toList();
    List<TodoView> completed =
        withAttachments(repo.findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(start, end))
            .stream()
            .sorted((a, b) -> compareInstantDesc(a.getCompletedAt(), b.getCompletedAt()))
            .map(this::toView)
            .toList();
    return new TodoDailyDetailView(date, created, completed);
  }

  @Transactional
  public void quickComplete(Long id) {
    complete(id, new TodoCompleteCommand(null, null));
  }

  @Transactional
  public void markDone(Long id) {
    quickComplete(id);
  }

  @Transactional
  public void markDone(Long id, String note, List<String> ids) {
    complete(id, new TodoCompleteCommand(note, ids));
  }

  @Transactional
  public void complete(Long id, TodoCompleteCommand command) {
    BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
    TodoTask task = getTask(id);
    BizAssert.state(
        task.getStatus() != TodoTaskStatus.DONE, ReminderError.TODO_STATUS_TRANSITION_INVALID);
    Set<String> before = new HashSet<>(task.allAttachmentFileIds());
    task.replaceCompletionAttachments(command.completionAttachmentFileIds());
    task.markDone(normalizeOptionalText(command.completionNote()));
    repo.save(task);
    persistAttachments(task);
    cleanupRemovedFiles(before, new HashSet<>(task.allAttachmentFileIds()));
    eventPublisher.publishEvent(TodoTaskReminderChangedEvent.removed(task.getId()));
  }

  @Transactional
  public void pause(Long id) {
    changeStatus(id, TodoTaskStatus.TODO, false);
  }

  @Transactional
  public void resume(Long id) {
    changeStatus(id, TodoTaskStatus.PAUSED, true);
  }

  @Transactional
  public void cancel(Long id) {
    pause(id);
  }

  @Transactional
  public void delete(Long id) {
    TodoTask task = getTask(id);
    Set<String> fileIds = new HashSet<>(task.allAttachmentFileIds());
    attachmentRepo.deleteByTodoId(id);
    repo.delete(task);
    fileIds.forEach(fileService::deleteFile);
    eventPublisher.publishEvent(TodoTaskReminderChangedEvent.removed(id));
  }

  private void changeStatus(Long id, TodoTaskStatus required, boolean resume) {
    TodoTask task = getTask(id);
    BizAssert.state(task.getStatus() == required, ReminderError.TODO_STATUS_TRANSITION_INVALID);
    if (resume) task.resume();
    else task.pause();
    repo.save(task);
    eventPublisher.publishEvent(
        resume
            ? TodoTaskReminderChangedEvent.updated(task.getId(), task.getRemindAt())
            : TodoTaskReminderChangedEvent.removed(task.getId()));
  }

  private PageData<TodoTask> withAttachments(PageData<TodoTask> page) {
    List<TodoTask> tasks = withAttachments(page.elements());
    return tasks.isEmpty()
        ? page
        : new PageData<>(
            page.pageNo(),
            page.pageSize(),
            page.numberOfElements(),
            page.totalPages(),
            page.totalElements(),
            tasks);
  }

  private List<TodoTask> withAttachments(List<TodoTask> tasks) {
    if (tasks == null || tasks.isEmpty()) return List.of();
    Map<Long, List<TodoAttachment>> map = new HashMap<>();
    List<Long> ids = tasks.stream().map(TodoTask::getId).filter(Objects::nonNull).toList();
    for (TodoAttachment attachment : attachmentRepo.findByTodoIdInOrderByTodoIdAscSortNoAsc(ids)) {
      map.computeIfAbsent(attachment.todoId(), key -> new ArrayList<>()).add(attachment);
    }
    tasks.forEach(task -> task.loadAttachments(map.get(task.getId())));
    return tasks;
  }

  private TodoTask getTask(Long id) {
    BizAssert.notNull(id, BaseError.MISSING_PARAMETER);
    TodoTask task =
        repo.findById(id).orElseThrow(() -> new BizException(ReminderError.TODO_NOT_FOUND));
    task.loadAttachments(attachmentRepo.findByTodoIdOrderBySortNoAsc(task.getId()));
    return task;
  }

  private void replaceAttachments(
      TodoTask task, List<String> contentIds, List<String> completionIds) {
    task.replaceContentAttachments(contentIds);
    task.replaceCompletionAttachments(completionIds);
  }

  private void persistAttachments(TodoTask task) {
    attachmentRepo.deleteByTodoId(task.getId());
    if (!task.attachments().isEmpty())
      attachmentRepo.saveAll(
          task.attachments().stream()
              .map(
                  item ->
                      TodoAttachment.create(
                          task.getId(),
                          item.stage(),
                          item.fileId(),
                          Objects.requireNonNullElse(item.sortNo(), 0L)))
              .toList());
  }

  private TodoView toView(TodoTask task) {
    return new TodoView(
        task.getId(),
        task.getContent(),
        task.getDueTime(),
        task.getStatus(),
        task.getNote(),
        task.contentAttachmentFileIds(),
        task.completionAttachmentFileIds(),
        task.getSortNo(),
        task.getCompletionNote(),
        task.getCompletedAt(),
        task.getCreatedAt());
  }

  private void applyStatus(TodoTask task, TodoTaskStatus target) {
    if (task.getStatus() == target) return;
    switch (target) {
      case TODO -> {
        BizAssert.state(
            task.getStatus() == TodoTaskStatus.PAUSED,
            ReminderError.TODO_STATUS_TRANSITION_INVALID);
        task.resume();
      }
      case PAUSED -> {
        BizAssert.state(
            task.getStatus() == TodoTaskStatus.TODO, ReminderError.TODO_STATUS_TRANSITION_INVALID);
        task.pause();
      }
      case DONE -> task.markDone(task.getCompletionNote());
      default -> throw new BizException(ReminderError.TODO_STATUS_TRANSITION_INVALID);
    }
  }

  private Map<LocalDate, Integer> buildDailyCountMap(
      List<TodoTask> tasks, ZoneId zoneId, boolean created) {
    Map<LocalDate, Integer> result = new LinkedHashMap<>();
    for (TodoTask task : tasks) {
      Instant time = created ? task.getCreatedAt() : task.getCompletedAt();
      if (time != null) result.merge(time.atZone(zoneId).toLocalDate(), 1, Integer::sum);
    }
    return result;
  }

  private int compareInstantDesc(Instant left, Instant right) {
    if (left == null) return right == null ? 0 : 1;
    return right == null ? -1 : right.compareTo(left);
  }

  private ZoneId defaultZoneId() {
    return ZoneId.of(
        Configs.get(SystemUserConfigSpecs.USER_PREFERENCE_DEFAULTS).timeZone().zoneId());
  }

  private String normalizeContent(String value) {
    String normalized = value == null ? null : value.trim();
    if (normalized == null || normalized.isEmpty())
      BizAssert.fail(ReminderError.TODO_CONTENT_REQUIRED);
    ensureTextLengthWithinLimit(normalized);
    return normalized;
  }

  private String normalizeOptionalText(String value) {
    if (value == null) return null;
    String normalized = value.trim();
    if (normalized.isEmpty()) return null;
    ensureTextLengthWithinLimit(normalized);
    return normalized;
  }

  private void ensureTextLengthWithinLimit(String value) {
    if (value != null && value.length() > TODO_TEXT_MAX_LENGTH)
      BizAssert.fail(ReminderError.TODO_TEXT_TOO_LONG);
  }

  private List<TodoTaskStatus> expandStatus(TodoTaskStatus status) {
    return status == null ? null : List.of(status);
  }

  private void cleanupRemovedFiles(Set<String> before, Set<String> after) {
    before.stream().filter(id -> !after.contains(id)).forEach(fileService::deleteFile);
  }

  private Long currentUserId() {
    AuthPrincipal principal = CtxUtil.getPrincipal();
    Long userId = principal == null ? null : principal.userId();
    BizAssert.notNull(userId, BaseError.FORBIDDEN);
    return userId;
  }
}

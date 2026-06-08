package com.corwin.reminder.application.service;

import com.corwin.framework.config.DefaultConfigWrapper;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 *
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
        String note = normalizeOptionalText(command.note());
        TodoTask t = TodoTask.create(content, command.dueTime());
        t.update(content, command.dueTime(), note);
        t.assignOwner(currentUserId());
        repo.save(t);
        replaceAttachments(t, command.contentAttachmentFileIds(), command.completionAttachmentFileIds());
        persistAttachments(t);
        eventPublisher.publishEvent(TodoTaskReminderChangedEvent.updated(t.getId(), t.getRemindAt()));
        return t.getId();
    }

    @Transactional
    public void update(Long id, TodoSaveCommand command) {
        Objects.requireNonNull(id, "id required");
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);

        TodoTask t = getTask(id);
        Set<String> before = new HashSet<>(t.allAttachmentFileIds());
        String content = normalizeContent(command.content());
        String note = normalizeOptionalText(command.note());
        t.update(content, command.dueTime(), note);
        repo.save(t);
        replaceAttachments(t, command.contentAttachmentFileIds(), command.completionAttachmentFileIds());
        persistAttachments(t);
        cleanupRemovedFiles(before, new HashSet<>(t.allAttachmentFileIds()));
        eventPublisher.publishEvent(TodoTaskReminderChangedEvent.updated(t.getId(), t.getRemindAt()));
    }

    public TodoView get(Long id) {
        return toView(getTask(id));
    }

    public PageData<TodoView> page(TodoTaskStatus status, String contentLike, PageSpec spec) {
        PageSpec effectiveSpec = resolvePageSpec(spec);
        List<TodoTaskStatus> statuses = expandStatus(status);
        TodoTaskPageQuery query = new TodoTaskPageQuery(statuses, StrUtil.trimToNull(contentLike));
        PageData<TodoTask> page = withAttachments(repo.pageByQuery(query, effectiveSpec));
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), page.elements().stream().map(this::toView).toList());
    }

    @Transactional
    public void reorder(TodoReorderCommand command) {
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);
        BizAssert.notEmpty(command.groups(), BaseError.MISSING_PARAMETER);

        List<TodoReorderCommand.GroupCommand> groups = command.groups().stream().filter(Objects::nonNull).toList();
        BizAssert.notEmpty(groups, BaseError.MISSING_PARAMETER);

        LinkedHashSet<Long> todoIds = new LinkedHashSet<>();
        for (TodoReorderCommand.GroupCommand group : groups) {
            BizAssert.notNull(group.status(), BaseError.MISSING_PARAMETER);
            BizAssert.notEmpty(group.orderedIds(), BaseError.MISSING_PARAMETER);
            for (Long id : group.orderedIds()) {
                BizAssert.notNull(id, BaseError.MISSING_PARAMETER);
                BizAssert.state(todoIds.add(id), BaseError.CONFLICT);
            }
        }

        List<TodoTask> tasks = withAttachments(repo.findByIdIn(List.copyOf(todoIds)));
        BizAssert.state(tasks.size() == todoIds.size(), ReminderError.TODO_NOT_FOUND);

        Map<Long, TodoTask> taskMap = new LinkedHashMap<>();
        tasks.forEach(task -> taskMap.put(task.getId(), task));
        List<TodoTask> updated = new ArrayList<>();
        for (TodoReorderCommand.GroupCommand group : groups) {
            TodoTaskStatus targetStatus = group.status();
            List<Long> orderedIds = group.orderedIds();
            for (int i = 0; i < orderedIds.size(); i++) {
                TodoTask task = taskMap.get(orderedIds.get(i));
                if (task == null) {
                    throw new BizException(ReminderError.TODO_NOT_FOUND);
                }
                applyStatus(task, targetStatus);
                task.applySortNo((long) i);
                updated.add(task);
            }
        }
        repo.saveAll(updated);
        for (TodoTask task : updated) {
            if (task.getStatus() == TodoTaskStatus.TODO) {
                eventPublisher.publishEvent(TodoTaskReminderChangedEvent.updated(task.getId(), task.getRemindAt()));
            } else {
                eventPublisher.publishEvent(TodoTaskReminderChangedEvent.removed(task.getId()));
            }
        }
    }

    public List<TodoDailyStatsView> dailyStats(LocalDate startDate, LocalDate endDate) {
        BizAssert.notNull(startDate, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(endDate, BaseError.MISSING_PARAMETER);
        BizAssert.state(!endDate.isBefore(startDate), BaseError.ILLEGAL_ARGUMENT);

        ZoneId zoneId = DefaultConfigWrapper.timeZone().toZoneId();
        Instant startInclusive = startDate.atStartOfDay(zoneId).toInstant();
        Instant endExclusive = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        Map<LocalDate, Integer> createdCountMap = buildDailyCountMap(
                repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startInclusive, endExclusive), zoneId, true);
        Map<LocalDate, Integer> completedCountMap = buildDailyCountMap(
                repo.findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(startInclusive, endExclusive), zoneId,
                false);

        List<TodoDailyStatsView> result = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            result.add(new TodoDailyStatsView(cursor, createdCountMap.getOrDefault(cursor, 0),
                    completedCountMap.getOrDefault(cursor, 0)));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    public TodoDailyDetailView dailyDetail(LocalDate date) {
        BizAssert.notNull(date, BaseError.MISSING_PARAMETER);

        ZoneId zoneId = DefaultConfigWrapper.timeZone().toZoneId();
        Instant startInclusive = date.atStartOfDay(zoneId).toInstant();
        Instant endExclusive = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        List<TodoView> createdItems = withAttachments(
                repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startInclusive, endExclusive)).stream()
                .sorted((left, right) -> compareInstantDesc(left.getCreatedAt(), right.getCreatedAt()))
                .map(this::toView).toList();
        List<TodoView> completedItems = withAttachments(
                repo.findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(startInclusive, endExclusive)).stream()
                .sorted((left, right) -> compareInstantDesc(left.getCompletedAt(), right.getCompletedAt()))
                .map(this::toView).toList();
        return new TodoDailyDetailView(date, createdItems, completedItems);
    }

    @Transactional
    public void quickComplete(Long id) {
        complete(id, new TodoCompleteCommand(null, null));
    }

    @Transactional
    public void complete(Long id, TodoCompleteCommand command) {
        BizAssert.notNull(command, BaseError.MISSING_PARAMETER);

        TodoTask t = getTask(id);
        BizAssert.state(t.getStatus() != TodoTaskStatus.DONE, ReminderError.TODO_STATUS_TRANSITION_INVALID);
        Set<String> before = new HashSet<>(t.allAttachmentFileIds());
        String completionNote = normalizeOptionalText(command.completionNote());
        t.replaceCompletionAttachments(command.completionAttachmentFileIds());
        t.markDone(completionNote);
        repo.save(t);
        persistAttachments(t);
        cleanupRemovedFiles(before, new HashSet<>(t.allAttachmentFileIds()));
        eventPublisher.publishEvent(TodoTaskReminderChangedEvent.removed(t.getId()));
    }

    @Transactional
    public void pause(Long id) {
        TodoTask t = getTask(id);
        BizAssert.state(t.getStatus() == TodoTaskStatus.TODO, ReminderError.TODO_STATUS_TRANSITION_INVALID);
        t.pause();
        repo.save(t);
        eventPublisher.publishEvent(TodoTaskReminderChangedEvent.removed(t.getId()));
    }

    @Transactional
    public void resume(Long id) {
        TodoTask t = getTask(id);
        BizAssert.state(t.getStatus() == TodoTaskStatus.PAUSED, ReminderError.TODO_STATUS_TRANSITION_INVALID);
        t.resume();
        repo.save(t);
        eventPublisher.publishEvent(TodoTaskReminderChangedEvent.updated(t.getId(), t.getRemindAt()));
    }

    @Transactional
    public void cancel(Long id) {
        pause(id);
    }

    @Transactional
    public void delete(Long id) {
        TodoTask t = getTask(id);
        Set<String> allFileIds = new HashSet<>(t.allAttachmentFileIds());
        attachmentRepo.deleteByTodoId(id);
        repo.delete(t);
        allFileIds.forEach(fileService::deleteFile);
        eventPublisher.publishEvent(TodoTaskReminderChangedEvent.removed(id));
    }

    @Transactional
    public void markDone(Long id) {
        quickComplete(id);
    }

    @Transactional
    public void markDone(Long id, String completionNote, List<String> completionAttachmentFileIds) {
        complete(id, new TodoCompleteCommand(completionNote, completionAttachmentFileIds));
    }

    private PageData<TodoTask> withAttachments(PageData<TodoTask> page) {
        List<TodoTask> tasks = withAttachments(page.elements());
        if (tasks.isEmpty()) {
            return page;
        }

        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), tasks);
    }

    private List<TodoTask> withAttachments(List<TodoTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        Map<Long, List<TodoAttachment>> attachmentMap = new HashMap<>();
        List<Long> todoIds = tasks.stream().map(TodoTask::getId).filter(Objects::nonNull).toList();
        for (TodoAttachment attachment : attachmentRepo.findByTodoIdInOrderByTodoIdAscSortNoAsc(todoIds)) {
            attachmentMap.computeIfAbsent(attachment.todoId(), key -> new ArrayList<>()).add(attachment);
        }
        tasks.forEach(task -> task.loadAttachments(attachmentMap.get(task.getId())));
        return tasks;
    }

    private void loadAttachments(TodoTask task) {
        task.loadAttachments(attachmentRepo.findByTodoIdOrderBySortNoAsc(task.getId()));
    }

    private TodoTask getTask(Long id) {
        BizAssert.notNull(id, BaseError.MISSING_PARAMETER);
        TodoTask task = repo.findById(id).orElseThrow(() -> new BizException(ReminderError.TODO_NOT_FOUND));
        loadAttachments(task);
        return task;
    }

    private void replaceAttachments(TodoTask task, List<String> contentAttachmentFileIds,
            List<String> completionAttachmentFileIds) {
        task.replaceContentAttachments(contentAttachmentFileIds);
        task.replaceCompletionAttachments(completionAttachmentFileIds);
    }

    private void persistAttachments(TodoTask task) {
        attachmentRepo.deleteByTodoId(task.getId());
        if (!task.attachments().isEmpty()) {
            List<TodoAttachment> newAttachments = task.attachments().stream()
                    .map(item -> TodoAttachment.create(task.getId(), item.stage(), item.fileId(),
                            Objects.requireNonNullElse(item.sortNo(), 0L))).toList();
            attachmentRepo.saveAll(newAttachments);
        }
    }

    private TodoView toView(TodoTask task) {
        return new TodoView(task.getId(), task.getContent(), task.getDueTime(), task.getStatus(), task.getNote(),
                task.contentAttachmentFileIds(), task.completionAttachmentFileIds(), task.getSortNo(),
                task.getCompletionNote(), task.getCompletedAt(), task.getCreatedAt());
    }

    private PageSpec resolvePageSpec(PageSpec spec) {
        if (spec == null || spec.sorts().isEmpty()) {
            return PageSpec.of(spec == null ? null : spec.pageNo(), spec == null ? null : spec.pageSize(),
                    List.of(new SortSpec("sortNo", SortDirection.ASC), new SortSpec("createdAt", SortDirection.DESC)));
        }
        return spec;
    }

    private void applyStatus(TodoTask task, TodoTaskStatus targetStatus) {
        TodoTaskStatus currentStatus = task.getStatus();
        if (currentStatus == targetStatus) {
            return;
        }
        switch (targetStatus) {
            case TODO -> {
                BizAssert.state(currentStatus == TodoTaskStatus.PAUSED, ReminderError.TODO_STATUS_TRANSITION_INVALID);
                task.resume();
            }
            case PAUSED -> {
                BizAssert.state(currentStatus == TodoTaskStatus.TODO, ReminderError.TODO_STATUS_TRANSITION_INVALID);
                task.pause();
            }
            case DONE -> {
                BizAssert.state(true, ReminderError.TODO_STATUS_TRANSITION_INVALID);
                task.markDone(task.getCompletionNote());
            }
            default -> throw new BizException(ReminderError.TODO_STATUS_TRANSITION_INVALID);
        }
    }

    private Map<LocalDate, Integer> buildDailyCountMap(List<TodoTask> tasks, ZoneId zoneId, boolean useCreatedAt) {
        Map<LocalDate, Integer> countMap = new LinkedHashMap<>();
        for (TodoTask task : tasks) {
            Instant time = useCreatedAt ? task.getCreatedAt() : task.getCompletedAt();
            if (time == null) {
                continue;
            }
            LocalDate date = time.atZone(zoneId).toLocalDate();
            countMap.merge(date, 1, Integer::sum);
        }
        return countMap;
    }

    private int compareInstantDesc(Instant left, Instant right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? null : content.trim();
        if (normalized == null || normalized.isEmpty()) {
            BizAssert.fail(ReminderError.TODO_CONTENT_REQUIRED);
        }
        ensureTextLengthWithinLimit(normalized);
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        ensureTextLengthWithinLimit(normalized);
        return normalized;
    }

    private void ensureTextLengthWithinLimit(String value) {
        if (value != null && value.length() > TODO_TEXT_MAX_LENGTH) {
            BizAssert.fail(ReminderError.TODO_TEXT_TOO_LONG);
        }
    }

    private List<TodoTaskStatus> expandStatus(TodoTaskStatus status) {
        if (status == null) {
            return null;
        }
        return List.of(status);
    }

    private void cleanupRemovedFiles(Set<String> before, Set<String> after) {
        for (String fileId : before) {
            if (!after.contains(fileId)) {
                fileService.deleteFile(fileId);
            }
        }
    }

    private Long currentUserId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        Long userId = principal == null ? null : principal.userId();
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        return userId;
    }
}

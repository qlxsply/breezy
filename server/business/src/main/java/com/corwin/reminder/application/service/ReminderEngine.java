package com.corwin.reminder.application.service;

import com.corwin.framework.concurrency.DelayedElement;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.util.HighDate;
import com.corwin.reminder.application.event.ScheduleReminderChangedEvent;
import com.corwin.reminder.application.event.TodoTaskReminderChangedEvent;
import com.corwin.reminder.application.port.NextOccurrenceCalculator;
import com.corwin.reminder.application.port.ReminderDispatchResult;
import com.corwin.reminder.domain.model.*;
import com.corwin.reminder.domain.repo.ReminderOutboxRepository;
import com.corwin.reminder.domain.repo.ScheduleEventRepository;
import com.corwin.reminder.domain.repo.TodoTaskRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * 提醒引擎：负责到点扫描与分发
 *
 * @author Corwin 2026/1/12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderEngine {

    private static final int PRELOAD_HOURS = 2;

    private final TodoTaskRepository todoRepo;
    private final ScheduleEventRepository scheduleRepo;
    private final ReminderOutboxRepository outboxRepo;
    private final NextOccurrenceCalculator nextOccurrenceCalculator;
    private final ReminderInternalService reminderInternalService;
    private final TodoReminderProcessor todoReminderProcessor;
    private final ScheduleReminderProcessor scheduleReminderProcessor;

    @PostConstruct
    public void init() {
        preloadReminders();
    }

    /**
     * 预加载未来一段时间内的提醒到延迟队列
     */
    public void preloadReminders() {
        Instant now = HighDate.mockInstant();
        Instant end = now.plus(PRELOAD_HOURS, ChronoUnit.HOURS);

        log.info("Preloading reminders before {}", end);

        // 1) Todo 预加载
        preloadTodoReminders(end);

        // 2) Schedule 预加载
        preloadScheduleReminders(now);
    }

    private void preloadTodoReminders(Instant end) {
        int pageNo = 1;
        while (true) {
            PageSpec spec = PageSpec.of(pageNo, 500, List.of());
            PageData<TodoTask> page = todoRepo.findByStatusInAndRemindAtLessThanEqual(List.of(TodoTaskStatus.TODO), end,
                    spec);

            for (TodoTask task : page.elements()) {
                submitTodoToProcessor(task);
            }

            if (pageNo >= page.totalPages()) {
                break;
            }
            pageNo++;
        }
    }

    private void preloadScheduleReminders(Instant now) {
        int pageNo = 1;
        while (true) {
            PageSpec spec = PageSpec.of(pageNo, 500, List.of());
            PageData<ScheduleEvent> page = scheduleRepo.findByStatus(ScheduleEventStatus.ACTIVE, spec);

            for (ScheduleEvent event : page.elements()) {
                submitNextScheduleReminder(event, now);
            }

            if (pageNo >= page.totalPages()) {
                break;
            }
            pageNo++;
        }
    }

    @EventListener
    public void onTodoReminderChanged(TodoTaskReminderChangedEvent event) {
        if (event.removed()) {
            todoReminderProcessor.cancel(event.todoId());
        } else {
            Instant now = HighDate.mockInstant();
            Instant end = now.plus(PRELOAD_HOURS, ChronoUnit.HOURS);
            if (event.remindAt() != null && !event.remindAt().isAfter(end)) {
                todoReminderProcessor.submit(new DelayedElement<>(event.todoId(), event.remindAt()));
            }
        }
    }

    @EventListener
    public void onScheduleReminderChanged(ScheduleReminderChangedEvent event) {
        if (event.removed()) {
            scheduleReminderProcessor.cancel(event.scheduleId());
        } else {
            scheduleRepo.findById(event.scheduleId())
                    .ifPresent(e -> submitNextScheduleReminder(e, HighDate.mockInstant()));
        }
    }

    /**
     * 兜底扫描任务
     */
    @Scheduled(fixedDelay = 60_000L) // 降低频率，仅作为延迟队列失效时的兜底
    @Transactional
    public void tick() {
        Instant now = HighDate.mockInstant();
        generateTodoOutbox(now);
        generateScheduleOutbox(now);
    }

    /**
     * 触发 Todo 提醒
     */
    @Transactional
    public void fireTodoReminder(TodoTask t) {
        if (t.getRemindAt() == null || t.getStatus() != TodoTaskStatus.TODO) {
            return;
        }

        String dedupeKey = "TODO|" + t.getId() + "|" + t.getRemindAt();
        ReminderOutbox outbox = createOutboxIfAbsent(dedupeKey, SourceType.TODO, t.getId(), t.getRemindAt(),
                t.getOwnerId(), t.getContent(), t.getNote());
        if (outbox != null) {
            ReminderDispatchResult dispatchResult = reminderInternalService.sendReminder(t.getOwnerId(),
                    "待办提醒: " + t.getContent(), t.getNote(), "/todo", outbox.getId());
            linkOutboxDelivery(outbox, dispatchResult);
        }
    }

    /**
     * 触发 Schedule 提醒
     */
    @Transactional
    public void fireScheduleReminder(ScheduleEvent e) {
        if (e.getStatus() != ScheduleEventStatus.ACTIVE) {
            return;
        }

        Instant now = HighDate.mockInstant();
        // 处理当前到点的提醒（可能由于 take 延迟，此时可能已经有多个点到期，但 DelayQueue 保证了按序触发）
        Instant nextStart = nextOccurrenceCalculator.nextStart(e, now.minusSeconds(1)); // 稍微往前偏一点点确保能算到当前的 occurrence
        if (nextStart != null) {
            List<Integer> advanceList = e.getAdvanceSecondsList();
            if (advanceList != null) {
                for (Integer adv : advanceList) {
                    Instant due = nextStart.minusSeconds(adv);
                    // 如果这个点已经到期了（且在 1 分钟内的，避免陈旧提醒），则尝试发送
                    if (!due.isAfter(now) && due.isAfter(now.minusSeconds(60))) {
                        String dedupeKey = "SCHEDULE|" + e.getId() + "|" + nextStart + "|" + adv;
                        ReminderOutbox outbox = createOutboxIfAbsent(dedupeKey, SourceType.SCHEDULE, e.getId(), due,
                                e.getOwnerId(), e.getTitle(), e.getNote());
                        if (outbox != null) {
                            ReminderDispatchResult dispatchResult = reminderInternalService.sendReminder(e.getOwnerId(),
                                    "日程提醒: " + e.getTitle(), e.getNote(), null, outbox.getId());
                            linkOutboxDelivery(outbox, dispatchResult);
                        }
                    }
                }
            }
        }

        // 无论是否发送成功，都要安排下一个提醒点
        submitNextScheduleReminder(e, now.plusMillis(1)); // 从现在之后的一毫秒开始找
    }

    private ReminderOutbox createOutboxIfAbsent(String dedupeKey, SourceType type, Long sourceId, Instant dueTime,
            Long ownerId, String title, String body) {
        if (outboxRepo.findByDedupeKey(dedupeKey).isPresent()) {
            return null;
        }
        ReminderOutbox r = ReminderOutbox.pending(ownerId, dedupeKey, type, sourceId, dueTime, title, body);
        return outboxRepo.save(r);
    }

    private void linkOutboxDelivery(ReminderOutbox outbox, ReminderDispatchResult dispatchResult) {
        if (dispatchResult == null || dispatchResult.deliveryId() == null) {
            return;
        }
        outbox.bindDelivery(dispatchResult.deliveryId());
        if (dispatchResult.delivered()) {
            outbox.markSent();
        }
        outboxRepo.save(outbox);
    }

    private void submitNextScheduleReminder(ScheduleEvent e, Instant now) {
        if (e.getStatus() != ScheduleEventStatus.ACTIVE) {
            return;
        }

        // 计算下一个提醒点
        // 算法：找到 nextStart，遍历 advanceList 找到第一个 due > now 的。
        // 如果 nextStart 的所有 due 都 <= now，则找再下一个 occurrence。

        Instant searchFrom = now;
        for (int i = 0; i < 5; i++) { // 最多往后找 5 个周期，避免死循环
            Instant nextStart = nextOccurrenceCalculator.nextStart(e, searchFrom);
            if (nextStart == null) {
                break;
            }

            List<Integer> advList = e.getAdvanceSecondsList();
            if (advList == null || advList.isEmpty()) {
                break;
            }

            // 提醒点按时间先后排序（adv 越大的点越早触发）
            List<Integer> sortedAdv = new java.util.ArrayList<>(advList);
            sortedAdv.sort(Collections.reverseOrder());

            for (Integer adv : sortedAdv) {
                Instant due = nextStart.minusSeconds(adv);
                if (due.isAfter(now)) {
                    // 找到了未来最近的一个点
                    scheduleReminderProcessor.submit(new DelayedElement<>(e.getId(), due));
                    return;
                }
            }
            // 当前周期的提醒点都过时了，从下个周期的开始时间继续找
            searchFrom = nextStart.plusMillis(1);
        }
    }

    private void generateTodoOutbox(Instant now) {
        PageSpec spec = PageSpec.of(1, 100, List.of());
        var page = todoRepo.findByStatusInAndRemindAtLessThanEqual(List.of(TodoTaskStatus.TODO), now, spec);
        for (TodoTask t : page.elements()) {
            fireTodoReminder(t);
        }
    }

    private void generateScheduleOutbox(Instant now) {
        PageSpec spec = PageSpec.of(1, 100, List.of());
        var page = scheduleRepo.findByStatus(ScheduleEventStatus.ACTIVE, spec);
        for (ScheduleEvent e : page.elements()) {
            // 这里作为兜底，仅处理当前 1 分钟内应该触发但可能由于重启等原因错过的
            Instant nextStart = nextOccurrenceCalculator.nextStart(e, now);
            if (nextStart == null) {
                continue;
            }

            List<Integer> advanceList = e.getAdvanceSecondsList();
            if (advanceList == null) {
                continue;
            }

            for (Integer adv : advanceList) {
                Instant due = nextStart.minusSeconds(adv);
                if (!due.isAfter(now) && due.isAfter(now.minusSeconds(60))) {
                    fireScheduleReminder(e);
                    break;
                }
            }
        }
    }

    private void submitTodoToProcessor(TodoTask task) {
        if (task.getRemindAt() != null && task.getStatus() == TodoTaskStatus.TODO) {
            todoReminderProcessor.submit(new DelayedElement<>(task.getId(), task.getRemindAt()));
        }
    }
}

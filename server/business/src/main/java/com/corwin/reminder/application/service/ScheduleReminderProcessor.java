package com.corwin.reminder.application.service;

import com.corwin.framework.concurrency.DelayQueueProcessor;
import com.corwin.reminder.domain.model.ScheduleEvent;
import com.corwin.reminder.domain.model.ScheduleEventStatus;
import com.corwin.reminder.domain.repo.ScheduleEventRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 日程提醒延迟处理器
 *
 * @author Corwin 2026/3/16
 */
@Slf4j
@Component
public class ScheduleReminderProcessor extends DelayQueueProcessor<Long> {

  private final ScheduleEventRepository scheduleRepo;
  private final ReminderEngine reminderEngine;

  public ScheduleReminderProcessor(
      ScheduleEventRepository scheduleRepo, @Lazy ReminderEngine reminderEngine) {
    super("ScheduleReminder");
    this.scheduleRepo = scheduleRepo;
    this.reminderEngine = reminderEngine;
  }

  @Override
  protected void processTask(Long scheduleId) {
    Optional<ScheduleEvent> eventOpt = scheduleRepo.findById(scheduleId);
    if (eventOpt.isEmpty()) {
      return;
    }
    ScheduleEvent event = eventOpt.get();
    if (event.getStatus() != ScheduleEventStatus.ACTIVE) {
      return;
    }

    // 调用 Engine 执行具体的触发逻辑，并自动安排下一个提醒点
    reminderEngine.fireScheduleReminder(event);
  }
}

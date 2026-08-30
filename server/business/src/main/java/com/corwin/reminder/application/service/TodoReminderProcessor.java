package com.corwin.reminder.application.service;

import com.corwin.framework.concurrency.DelayQueueProcessor;
import com.corwin.reminder.domain.model.TodoTask;
import com.corwin.reminder.domain.model.TodoTaskStatus;
import com.corwin.reminder.domain.repo.TodoTaskRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 待办事项提醒延迟处理器
 *
 * @author Corwin 2026/3/16
 */
@Slf4j
@Component
public class TodoReminderProcessor extends DelayQueueProcessor<Long> {

  private final TodoTaskRepository todoRepo;
  private final ReminderEngine reminderEngine;

  public TodoReminderProcessor(TodoTaskRepository todoRepo, @Lazy ReminderEngine reminderEngine) {
    super("TodoReminder");
    this.todoRepo = todoRepo;
    this.reminderEngine = reminderEngine;
  }

  @Override
  protected void processTask(Long todoId) {
    Optional<TodoTask> taskOpt = todoRepo.findById(todoId);
    if (taskOpt.isEmpty()) {
      return;
    }
    TodoTask task = taskOpt.get();
    if (task.getStatus() != TodoTaskStatus.TODO) {
      return;
    }

    // 调用 Engine 执行具体的提醒逻辑（含去重、写入 Outbox 和推送）
    reminderEngine.fireTodoReminder(task);
  }
}

package com.corwin.system.task.application.service;

import com.corwin.framework.scheduling.InternalTask;
import com.corwin.system.task.application.view.TaskView;
import com.corwin.system.task.domain.model.TaskConfig;
import com.corwin.system.task.domain.model.TaskDefinition;
import com.corwin.system.task.domain.model.TaskStatus;
import com.corwin.system.task.domain.repo.TaskConfigRepository;
import com.corwin.system.task.domain.repo.TaskDefinitionRepository;
import com.corwin.system.task.infrastructure.scheduling.TaskSchedulerManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for task management operations. Handles task definition synchronization,
 * lifecycle management (publish/stop/run), configuration updates, and startup scheduling.
 *
 * @author Corwin 2026/3/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAppService {

  private final ApplicationContext applicationContext;
  private final TaskDefinitionRepository taskDefinitionRepository;
  private final TaskConfigRepository taskConfigRepository;
  private final TaskSchedulerManager taskSchedulerManager;

  /**
   * Scans all Spring beans for {@link InternalTask} annotations, synchronizes the detected task
   * definitions with the database, and marks removed tasks accordingly.
   */
  @Transactional
  public void syncTaskDefinitions() {
    log.info("[task-sync] starting task definition scan...");

    String[] beanNames = applicationContext.getBeanDefinitionNames();

    List<ScannedTask> scannedTasks = new ArrayList<>();
    for (String beanName : beanNames) {
      try {
        Object bean = applicationContext.getBean(beanName);
        Class<?> targetClass = bean.getClass();
        for (Method method : targetClass.getDeclaredMethods()) {
          if (method.isAnnotationPresent(InternalTask.class)) {
            InternalTask ann = method.getAnnotation(InternalTask.class);
            scannedTasks.add(
                new ScannedTask(
                    ann.code(), ann.name(), ann.description(), beanName, method.getName()));
          }
        }
      } catch (Exception e) {
        // Ignore
      }
    }

    List<TaskDefinition> existingDefs = taskDefinitionRepository.findAllByRemovedFalse();
    Map<String, TaskDefinition> defMap =
        existingDefs.stream().collect(Collectors.toMap(TaskDefinition::getCode, d -> d));

    Set<String> scannedCodes =
        scannedTasks.stream().map(ScannedTask::code).collect(Collectors.toSet());

    for (ScannedTask scanned : scannedTasks) {
      TaskDefinition def = defMap.get(scanned.code());
      if (def == null) {
        log.info("[task-sync] detected new task: {}", scanned.code());
        TaskDefinition newDef =
            TaskDefinition.create(
                scanned.code(),
                scanned.name(),
                scanned.description(),
                scanned.beanName(),
                scanned.methodName());
        taskDefinitionRepository.save(newDef);

        if (!taskConfigRepository.existsById(scanned.code())) {
          taskConfigRepository.save(TaskConfig.createDefault(scanned.code()));
        }
      } else {
        def.updateDetails(
            scanned.name(), scanned.description(), scanned.beanName(), scanned.methodName());
        taskDefinitionRepository.save(def);
      }
    }

    for (TaskDefinition def : existingDefs) {
      if (!scannedCodes.contains(def.getCode())) {
        log.warn(
            "[task-sync] task code {} no longer exists in code, marking as removed", def.getCode());
        def.markRemoved();
        taskDefinitionRepository.save(def);

        taskConfigRepository
            .findById(def.getCode())
            .ifPresent(
                config -> {
                  if (config.getTaskStatus() == TaskStatus.PUBLISHED) {
                    config.markDisabled();
                    taskConfigRepository.save(config);
                    taskSchedulerManager.cancelTask(def.getCode());
                  }
                });
      }
    }

    log.info("[task-sync] scan completed. total scanned: {}", scannedTasks.size());
  }

  /** Loads all published task configurations and schedules them for periodic execution. */
  public void startupAllTasks() {
    log.info("[task-startup] loading published tasks...");
    List<TaskConfig> publishedConfigs =
        taskConfigRepository.findAllByTaskStatus(TaskStatus.PUBLISHED);

    int count = 0;
    for (TaskConfig config : publishedConfigs) {
      var defOpt = taskDefinitionRepository.findById(config.getCode());
      if (defOpt.isPresent()) {
        var def = defOpt.get();
        if (!def.isRemoved() && config.getCronExpr() != null && !config.getCronExpr().isEmpty()) {
          taskSchedulerManager.scheduleTask(
              config.getCode(), config.getCronExpr(), def.getBeanName(), def.getMethodName());
          count++;
        }
      }
    }
    log.info("[task-startup] startup completed. total scheduled: {}", count);
  }

  /**
   * Lists all non-removed tasks with their current configuration status.
   *
   * @return a list of task views
   */
  public List<TaskView> listTasks() {
    List<TaskDefinition> defs = taskDefinitionRepository.findAllByRemovedFalse();
    return defs.stream()
        .map(
            def -> {
              TaskConfig config =
                  taskConfigRepository
                      .findById(def.getCode())
                      .orElseGet(() -> TaskConfig.createDefault(def.getCode()));
              return new TaskView(
                  def.getCode(),
                  def.getName(),
                  def.getDescription(),
                  def.getBeanName(),
                  def.getMethodName(),
                  def.isRemoved(),
                  config.getCronExpr(),
                  config.getTaskStatus(),
                  config.getLastRunAt(),
                  config.getLastRunStatus(),
                  config.getLastErrorMessage(),
                  def.getUpdatedAt());
            })
        .collect(Collectors.toList());
  }

  /**
   * Updates the cron expression for a task and re-publishes it if currently active.
   *
   * @param code the task code
   * @param cronExpr the new cron expression
   */
  @Transactional
  public void updateTaskConfig(String code, String cronExpr) {
    TaskConfig config =
        taskConfigRepository
            .findById(code)
            .orElseThrow(() -> new RuntimeException("Task config not found: " + code));
    config.updateCronExpr(cronExpr);
    taskConfigRepository.save(config);

    if (config.getTaskStatus() == TaskStatus.PUBLISHED) {
      publishTask(code);
    }
  }

  /**
   * Publishes a task, activating its cron-based scheduled execution.
   *
   * @param code the task code
   */
  @Transactional
  public void publishTask(String code) {
    TaskConfig config =
        taskConfigRepository
            .findById(code)
            .orElseThrow(() -> new RuntimeException("Task config not found: " + code));
    TaskDefinition def =
        taskDefinitionRepository
            .findById(code)
            .orElseThrow(() -> new RuntimeException("Task definition not found: " + code));

    if (config.getCronExpr() == null || config.getCronExpr().isEmpty()) {
      throw new RuntimeException("CRON expression is required for publishing");
    }

    config.markPublished();
    taskConfigRepository.save(config);

    taskSchedulerManager.scheduleTask(
        code, config.getCronExpr(), def.getBeanName(), def.getMethodName());
  }

  /**
   * Stops a task, disabling its scheduled execution.
   *
   * @param code the task code
   */
  @Transactional
  public void stopTask(String code) {
    TaskConfig config =
        taskConfigRepository
            .findById(code)
            .orElseThrow(() -> new RuntimeException("Task config not found: " + code));
    config.markDisabled();
    taskConfigRepository.save(config);

    taskSchedulerManager.cancelTask(code);
  }

  /**
   * Triggers immediate one-time execution of a task.
   *
   * @param code the task code
   */
  public void runTask(String code) {
    TaskDefinition def =
        taskDefinitionRepository
            .findById(code)
            .orElseThrow(() -> new RuntimeException("Task definition not found: " + code));

    taskSchedulerManager.executeNow(code, def.getBeanName(), def.getMethodName());
  }

  private record ScannedTask(
      String code, String name, String description, String beanName, String methodName) {}
}

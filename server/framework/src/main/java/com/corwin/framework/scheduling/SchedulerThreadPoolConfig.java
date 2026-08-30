package com.corwin.framework.scheduling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for the dedicated scheduled-task thread pool.
 *
 * <p>Separates scheduling infrastructure from the async task pool to avoid interference. Context is
 * managed by {@link SchedulerTaskDecorator} — HTTP request context is <b>not</b> propagated.
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/23
 */
@Configuration
@EnableScheduling
public class SchedulerThreadPoolConfig {

  /** Task decorator that initializes a fresh system identity for each scheduled execution. */
  @Bean
  public TaskDecorator schedulerTaskDecorator(SchedulerExecutionIdentityProvider identityProvider) {
    return new SchedulerTaskDecorator(identityProvider);
  }

  /**
   * The dedicated thread-pool-task-scheduler bean named {@code "taskScheduler"}.
   *
   * <p>Pool size = 4, with graceful shutdown waiting up to 10 seconds.
   *
   * @param schedulerTaskDecorator the scheduler context decorator
   * @return the configured scheduler
   */
  @Bean(name = "taskScheduler")
  public ThreadPoolTaskScheduler taskScheduler(TaskDecorator schedulerTaskDecorator) {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(4);
    scheduler.setThreadNamePrefix("task-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(10);
    scheduler.setTaskDecorator(schedulerTaskDecorator);
    scheduler.initialize();
    return scheduler;
  }
}

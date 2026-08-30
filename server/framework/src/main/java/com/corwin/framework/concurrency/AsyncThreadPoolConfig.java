package com.corwin.framework.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for the application-wide asynchronous thread pool.
 *
 * <p>Provides a unified {@link java.util.concurrent.Executor} for {@code @Async} methods and manual
 * async task submission with the following features:
 *
 * <ul>
 *   <li>Automatic Ctx + MDC context propagation via {@link ContextCopyingTaskDecorator}
 *   <li>Graceful shutdown waiting for in-flight tasks to complete
 *   <li>{@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy CallerRunsPolicy} when the
 *       queue is full
 * </ul>
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/19
 */
@Configuration
@EnableAsync
public class AsyncThreadPoolConfig {

  /** Task decorator that copies the calling thread's Ctx and MDC to the worker thread. */
  @Bean
  public TaskDecorator contextCopyingTaskDecorator() {
    return new ContextCopyingTaskDecorator();
  }

  /**
   * The default async thread pool bean named {@code "taskExecutor"}.
   *
   * <p>Core pool size = 4, max pool size = 16, queue capacity = 2000, with {@link
   * java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy} rejection policy.
   *
   * @param contextCopyingTaskDecorator the context-copying decorator
   * @return the configured executor
   */
  @Bean(name = "taskExecutor")
  public Executor taskExecutor(TaskDecorator contextCopyingTaskDecorator) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(2000);
    executor.setThreadNamePrefix("async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(10);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setTaskDecorator(contextCopyingTaskDecorator);
    executor.initialize();
    return executor;
  }
}

package com.corwin.framework.scheduling;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Binds the custom {@link ThreadPoolTaskScheduler} to Spring's {@code @Scheduled} infrastructure.
 * <p>
 * Spring defaults to a single-threaded scheduler; this configuration replaces it
 * with a multi-threaded pool defined in {@link SchedulerThreadPoolConfig}.
 * This class only establishes the binding — the pool lifecycle is managed externally.
 * <p>
 * Scheduled-task threads do <b>not</b> inherit HTTP request context;
 * {@link SchedulerTaskDecorator} initializes a fresh system context per execution.
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/23
 */
@Configuration
@AllArgsConstructor
public class SchedulerConfig implements SchedulingConfigurer {

    /**
     * The scheduler thread pool, injected from {@link SchedulerThreadPoolConfig}.
     */
    private final ThreadPoolTaskScheduler taskScheduler;

    /**
     * Replaces the default single-threaded scheduler with the configured thread pool.
     *
     * @param taskRegistrar the task registrar
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler);
    }
}

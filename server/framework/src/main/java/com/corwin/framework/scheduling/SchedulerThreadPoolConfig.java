package com.corwin.framework.scheduling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Spring 定时任务线程池配置。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>提供 {@code @Scheduled} 使用的专用调度线程池</li>
 *     <li>为定时任务绑定统一的调度上下文</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *     <li>定时任务线程池与异步线程池分离，避免相互影响</li>
 *     <li>定时任务不复制 HTTP 请求上下文</li>
 *     <li>定时任务上下文由 {@link SchedulerTaskDecorator} 统一初始化</li>
 * </ul>
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/23
 */
@Configuration
@EnableScheduling
public class SchedulerThreadPoolConfig {

    /**
     * 定时任务上下文装饰器。
     *
     * <p>为每次调度执行创建新的系统上下文，
     * 固定使用 scheduler 系统账号，不复制请求线程上下文。</p>
     *
     * @return TaskDecorator
     */
    @Bean
    public TaskDecorator schedulerTaskDecorator(SchedulerExecutionIdentityProvider identityProvider) {
        return new SchedulerTaskDecorator(identityProvider);
    }

    /**
     * 定时任务专用线程池。
     *
     * <p>参数说明：</p>
     * <ul>
     *     <li>poolSize = 4：调度线程数</li>
     *     <li>threadNamePrefix = scheduled-：线程名前缀</li>
     * </ul>
     *
     * @param schedulerTaskDecorator 定时任务上下文装饰器
     * @return ThreadPoolTaskScheduler
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

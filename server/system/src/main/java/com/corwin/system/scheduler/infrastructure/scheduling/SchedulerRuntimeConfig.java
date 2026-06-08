package com.corwin.system.scheduler.infrastructure.scheduling;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 动态任务运行时线程池配置。
 *
 * @author Corwin 2026/4/15
 */
@Configuration
public class SchedulerRuntimeConfig {

    @Bean(name = "schedulerJobTaskScheduler")
    public ThreadPoolTaskScheduler schedulerJobTaskScheduler(
            @Qualifier("schedulerTaskDecorator") TaskDecorator schedulerTaskDecorator) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(8);
        scheduler.setThreadNamePrefix("scheduler-job-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setTaskDecorator(schedulerTaskDecorator);
        scheduler.initialize();
        return scheduler;
    }
}

package com.corwin.framework.concurrency;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。
 *
 * <p>提供统一异步线程池，用于：</p>
 * <ul>
 *     <li>{@code @Async}</li>
 *     <li>业务手工提交异步任务</li>
 * </ul>
 *
 * <p>线程池特性：</p>
 * <ul>
 *     <li>支持 Ctx + MDC 上下文自动传播</li>
 *     <li>优雅停机时等待任务完成</li>
 *     <li>线程池满载时由调用线程执行任务（CallerRunsPolicy）</li>
 * </ul>
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/19
 */
@Configuration
@EnableAsync
public class AsyncThreadPoolConfig {

    /**
     * 任务上下文复制装饰器。
     *
     * <p>用于在线程切换时复制调用线程中的 Ctx 与 MDC。</p>
     *
     * @return TaskDecorator
     */
    @Bean
    public TaskDecorator contextCopyingTaskDecorator() {
        return new ContextCopyingTaskDecorator();
    }

    /**
     * 默认异步线程池。
     *
     * <p>参数说明：</p>
     * <ul>
     *     <li>corePoolSize = 4：核心线程数</li>
     *     <li>maxPoolSize = 16：最大线程数</li>
     *     <li>queueCapacity = 2000：任务队列容量</li>
     *     <li>CallerRunsPolicy：线程池满时由提交线程执行，降低任务丢失风险</li>
     * </ul>
     *
     * @param contextCopyingTaskDecorator 上下文复制装饰器
     * @return Executor
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
package com.corwin.framework.scheduling;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Spring 定时任务调度器绑定配置。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>显式指定 {@code @Scheduled} 注解使用的 {@link ThreadPoolTaskScheduler}</li>
 *     <li>将自定义调度线程池接入 Spring 调度体系</li>
 * </ul>
 *
 * <p>背景说明：</p>
 * <ul>
 *     <li>Spring 默认会使用一个单线程调度器执行所有 {@code @Scheduled} 任务</li>
 *     <li>在存在多个定时任务或任务执行时间较长的场景下，容易造成任务阻塞或串行执行</li>
 *     <li>因此通常需要自定义线程池，并显式绑定给调度系统</li>
 * </ul>
 *
 * <p>设计说明：</p>
 * <ul>
 *     <li>本类不负责创建线程池，仅负责“绑定关系”</li>
 *     <li>线程池由独立配置类（如 {@link SchedulerThreadPoolConfig}）定义</li>
 *     <li>通过构造注入方式获取已初始化的 {@link ThreadPoolTaskScheduler}</li>
 * </ul>
 *
 * <p>执行流程：</p>
 * <ol>
 *     <li>Spring 启动时扫描 {@code @EnableScheduling}</li>
 *     <li>初始化 {@link ScheduledTaskRegistrar}</li>
 *     <li>调用 {@link #configureTasks(ScheduledTaskRegistrar)}</li>
 *     <li>将自定义 {@code taskScheduler} 注入到调度器中</li>
 *     <li>所有 {@code @Scheduled} 方法将由该线程池调度执行</li>
 * </ol>
 *
 * <p>上下文说明：</p>
 * <ul>
 *     <li>定时任务线程不继承 HTTP 请求上下文</li>
 *     <li>上下文初始化由 {@code SchedulerTaskDecorator} 负责</li>
 *     <li>通常会设置固定系统账号（如 scheduler）及独立 traceId</li>
 * </ul>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>避免在本类中定义 {@code taskScheduler} Bean，否则可能引发循环依赖</li>
 *     <li>确保仅存在一个用于调度的 {@link ThreadPoolTaskScheduler}（或明确区分多个）</li>
 *     <li>线程池大小需根据任务数量和执行耗时合理配置</li>
 * </ul>
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/23
 */
@Configuration
@AllArgsConstructor
public class SchedulerConfig implements SchedulingConfigurer {

    /**
     * 定时任务调度线程池。
     *
     * <p>由独立配置类定义（通常命名为 taskScheduler）。</p>
     */
    private final ThreadPoolTaskScheduler taskScheduler;

    /**
     * 显式指定 {@code @Scheduled} 使用的调度器。
     *
     * <p>该方法会在 Spring 初始化调度任务时被调用，用于替换默认调度器。</p>
     *
     * @param taskRegistrar 调度任务注册器
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler);
    }
}

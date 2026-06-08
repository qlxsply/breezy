package com.corwin.framework.event.transport.inmemory.executor;

/**
 * 内存分发执行器抽象。
 * <p>
 * 用于将“订阅消费任务执行策略”从 transport 语义中解耦，
 * 便于在同一语义下切换线程池或 Disruptor 引擎。
 *
 * @author Corwin 2026/4/9
 */
public interface InMemoryDispatchExecutor {

    /**
     * 启动执行器。
     */
    void start();

    /**
     * 提交消费任务。
     *
     * @param task 待执行任务
     */
    void execute(Runnable task);

    /**
     * 关闭执行器。
     */
    void shutdown();
}


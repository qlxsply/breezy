package com.corwin.framework.event.transport.inmemory.executor;

/**
 * Interface for in-memory event dispatch — delivering events to all consumers
 * within a single process.
 * <p>
 * Allows implementations (sync/async/bounded/unbounded) to be swapped seamlessly.
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


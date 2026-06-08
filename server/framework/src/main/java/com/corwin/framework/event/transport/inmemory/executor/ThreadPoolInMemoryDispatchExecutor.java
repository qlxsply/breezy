package com.corwin.framework.event.transport.inmemory.executor;

import com.corwin.framework.event.config.AsyncEventProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于线程池的内存分发执行器。
 * <p>
 * 特点：
 * <ul>
 *     <li>通过配置控制核心线程、最大线程、队列容量与线程名前缀。</li>
 *     <li>拒绝策略使用 CallerRunsPolicy，在突发流量下提供背压。</li>
 *     <li>关闭阶段支持超时等待，超时后强制中断。</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
@Slf4j
public class ThreadPoolInMemoryDispatchExecutor implements InMemoryDispatchExecutor {

    private final AsyncEventProperties.InMemory.ThreadPool config;
    private ThreadPoolExecutor executor;

    public ThreadPoolInMemoryDispatchExecutor(AsyncEventProperties properties) {
        Objects.requireNonNull(properties, "properties required");
        this.config = Objects.requireNonNull(properties.getInMemory().getThreadPool(), "threadPool config required");
    }

    /**
     * 按配置创建并启动线程池。
     */
    @Override
    public synchronized void start() {
        if (executor != null) {
            return;
        }
        int coreSize = Math.max(1, config.getCorePoolSize());
        int maxSize = Math.max(coreSize, config.getMaxPoolSize());
        int queueCapacity = Math.max(1, config.getQueueCapacity());
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(queueCapacity);
        ThreadFactory threadFactory = new NamedThreadFactory(resolveThreadPrefix());
        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        executor = new ThreadPoolExecutor(coreSize, maxSize, Math.max(0L, config.getKeepAliveSeconds()),
                TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler);
        executor.allowCoreThreadTimeOut(false);
        log.info("Async event thread-pool executor started. coreSize={}, maxSize={}, queueCapacity={}", coreSize,
                maxSize, queueCapacity);
    }

    /**
     * 提交任务到线程池执行。
     */
    @Override
    public void execute(Runnable task) {
        Objects.requireNonNull(task, "task required");
        ThreadPoolExecutor runningExecutor = executor;
        if (runningExecutor == null) {
            throw new IllegalStateException("ThreadPoolInMemoryDispatchExecutor is not started");
        }
        runningExecutor.execute(task);
    }

    /**
     * 优雅关闭线程池，超时后强制 shutdownNow。
     */
    @Override
    public synchronized void shutdown() {
        ThreadPoolExecutor runningExecutor = executor;
        if (runningExecutor == null) {
            return;
        }
        executor = null;
        runningExecutor.shutdown();
        long timeoutMs = Math.max(1000L, config.getShutdownTimeoutMs());
        try {
            if (!runningExecutor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                runningExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            runningExecutor.shutdownNow();
        }
        log.info("Async event thread-pool executor stopped");
    }

    /**
     * 解析线程名前缀，未配置时使用默认前缀。
     */
    private String resolveThreadPrefix() {
        String configured = config.getThreadNamePrefix();
        if (configured == null || configured.isBlank()) {
            return "async-event-pool-";
        }
        return configured.trim();
    }

    /**
     * 线程命名工厂，便于排查异步线程来源。
     */
    private static final class NamedThreadFactory implements ThreadFactory {

        private final String prefix;
        private final AtomicInteger sequence = new AtomicInteger(1);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        /**
         * 创建守护线程并追加递增序号。
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}


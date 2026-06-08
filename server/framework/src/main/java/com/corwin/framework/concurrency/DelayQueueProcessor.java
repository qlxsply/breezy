package com.corwin.framework.concurrency;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 通用的延迟队列处理器基类
 *
 * @author Corwin 2026/3/16
 */
@Slf4j
public abstract class DelayQueueProcessor<T> {

    private final DelayQueue<DelayedElement<T>> delayQueue = new DelayQueue<>();
    private final ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final String processorName;

    protected DelayQueueProcessor(String processorName) {
        this.processorName = processorName;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, processorName + "-thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    @PostConstruct
    public void start() {
        executorService.execute(this::run);
        log.info("{} started.", processorName);
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("{} did not terminate in time.", processorName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("{} stopped.", processorName);
    }

    /**
     * 提交延迟任务
     */
    public void submit(DelayedElement<T> element) {
        // 先移除旧的（如果 payload 相同），保证堆中只有一个该任务的最新版
        delayQueue.remove(element);
        delayQueue.put(element);
        log.debug("{} task submitted: {}, delay: {}ms", processorName, element.payload(),
                element.getDelay(TimeUnit.MILLISECONDS));
    }

    /**
     * 取消延迟任务
     */
    public void cancel(T payload) {
        // 这里依赖 DelayedElement 的 equals 实现（仅比较 payload）
        delayQueue.remove(new DelayedElement<>(payload, java.time.Instant.EPOCH));
    }

    private void run() {
        while (running.get()) {
            try {
                DelayedElement<T> element = delayQueue.take();
                processTask(element.payload());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("{} process task error", processorName, e);
            }
        }
    }

    /**
     * 子类实现具体的业务逻辑
     */
    protected abstract void processTask(T payload);
}

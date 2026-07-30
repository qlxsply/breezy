package com.corwin.framework.event.transport.inmemory.executor;

import com.corwin.framework.event.config.AsyncEventProperties;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Disruptor-based in-memory dispatch executor.
 * <p>
 * Wraps consumption tasks as RingBuffer events for high-throughput, low-latency
 * scenarios. Currently supports only a single consumer thread per config; values
 * above 1 are degraded with a warning.
 *
 * @author Corwin 2026/4/9
 */
@Slf4j
public class DisruptorInMemoryDispatchExecutor implements InMemoryDispatchExecutor {

    private static final EventFactory<EventContainer> EVENT_FACTORY = EventContainer::new;

    private final AsyncEventProperties.InMemory.Disruptor config;
    private Disruptor<EventContainer> disruptor;
    private RingBuffer<EventContainer> ringBuffer;

    public DisruptorInMemoryDispatchExecutor(AsyncEventProperties properties) {
        Objects.requireNonNull(properties, "properties required");
        this.config = Objects.requireNonNull(properties.getInMemory().getDisruptor(), "disruptor config required");
    }

    /**
     * 初始化 Disruptor、注册事件处理器并启动 RingBuffer。
     */
    @Override
    public synchronized void start() {
        if (disruptor != null) {
            return;
        }
        int ringBufferSize = verifyRingBufferSize(config.getRingBufferSize());
        WaitStrategy waitStrategy = createWaitStrategy(config.getWaitStrategy());
        ProducerType producerType = createProducerType(config.getProducerType());
        int configuredConsumerThreads = Math.max(1, config.getConsumerThreads());
        if (configuredConsumerThreads > 1) {
            log.warn(
                    "Disruptor dispatch executor currently supports single consumer only. configuredConsumerThreads={} will fallback to 1",
                    configuredConsumerThreads);
        }
        this.disruptor = new Disruptor<>(EVENT_FACTORY, ringBufferSize, new ExecutorThreadFactory(), producerType,
                waitStrategy);
        this.disruptor.setDefaultExceptionHandler(new DispatchExceptionHandler());
        this.disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            Runnable task = event.task;
            if (task != null) {
                task.run();
            }
            event.clear();
        });
        this.ringBuffer = this.disruptor.start();
        log.info("Async event disruptor executor started. ringBufferSize={}, producerType={}, waitStrategy={}",
                ringBufferSize, producerType, config.getWaitStrategy());
    }

    /**
     * 发布任务到 RingBuffer，交由 Disruptor 消费线程执行。
     */
    @Override
    public void execute(Runnable task) {
        Objects.requireNonNull(task, "task required");
        RingBuffer<EventContainer> currentRingBuffer = ringBuffer;
        if (currentRingBuffer == null) {
            throw new IllegalStateException("DisruptorInMemoryDispatchExecutor is not started");
        }
        long sequence = currentRingBuffer.next();
        try {
            EventContainer container = currentRingBuffer.get(sequence);
            container.task = task;
        } finally {
            currentRingBuffer.publish(sequence);
        }
    }

    /**
     * 关闭 Disruptor，超时则强制 halt。
     */
    @Override
    public synchronized void shutdown() {
        Disruptor<EventContainer> currentDisruptor = disruptor;
        if (currentDisruptor == null) {
            return;
        }
        disruptor = null;
        ringBuffer = null;
        long timeoutMs = Math.max(1000L, config.getShutdownTimeoutMs());
        try {
            currentDisruptor.shutdown(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (com.lmax.disruptor.TimeoutException ex) {
            log.warn("Disruptor dispatch executor shutdown timeout. timeoutMs={}", timeoutMs, ex);
            currentDisruptor.halt();
        }
        log.info("Async event disruptor executor stopped");
    }

    /**
     * 校验 RingBuffer 大小合法性。
     */
    private int verifyRingBufferSize(int ringBufferSize) {
        if (ringBufferSize <= 0) {
            throw new IllegalArgumentException(
                    "framework.async-event.in-memory.disruptor.ring-buffer-size must be > 0");
        }
        if ((ringBufferSize & (ringBufferSize - 1)) != 0) {
            throw new IllegalArgumentException(
                    "framework.async-event.in-memory.disruptor.ring-buffer-size must be a power of two");
        }
        return ringBufferSize;
    }

    /**
     * 解析生产者模式配置。
     */
    private ProducerType createProducerType(String producerType) {
        String normalized = normalize(producerType, "MULTI");
        return switch (normalized) {
            case "SINGLE" -> ProducerType.SINGLE;
            case "MULTI" -> ProducerType.MULTI;
            default -> throw new IllegalArgumentException(
                    "Unsupported producerType: " + producerType + ". supported: SINGLE/MULTI");
        };
    }

    /**
     * 解析等待策略配置。
     */
    private WaitStrategy createWaitStrategy(String waitStrategy) {
        String normalized = normalize(waitStrategy, "BLOCKING");
        return switch (normalized) {
            case "BLOCKING" -> new BlockingWaitStrategy();
            case "YIELDING" -> new YieldingWaitStrategy();
            case "SLEEPING" -> new SleepingWaitStrategy();
            case "BUSY_SPIN" -> new BusySpinWaitStrategy();
            default -> throw new IllegalArgumentException(
                    "Unsupported waitStrategy: " + waitStrategy + ". supported: BLOCKING/YIELDING/SLEEPING/BUSY_SPIN");
        };
    }

    /**
     * 将配置文本归一化为大写枚举风格字符串。
     */
    private String normalize(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return defaultValue;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    /**
     * RingBuffer 事件容器，承载单个待执行任务。
     */
    private static final class EventContainer {
        private Runnable task;

        /**
         * 任务执行后清理引用，避免对象滞留。
         */
        private void clear() {
            this.task = null;
        }
    }

    /**
     * Disruptor 线程工厂，统一命名并创建守护线程。
     */
    private static final class ExecutorThreadFactory implements ThreadFactory {

        private final AtomicInteger sequence = new AtomicInteger(1);

        /**
         * 创建新的 Disruptor 消费线程。
         */
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "async-event-disruptor-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     * Disruptor 异常处理器，统一输出启动/运行/关闭阶段异常日志。
     */
    private static final class DispatchExceptionHandler implements ExceptionHandler<EventContainer> {

        /**
         * 处理事件消费阶段异常。
         */
        @Override
        public void handleEventException(Throwable ex, long sequence, EventContainer event) {
            log.warn("Disruptor dispatch task failed. sequence={}", sequence, ex);
        }

        /**
         * 处理启动阶段异常。
         */
        @Override
        public void handleOnStartException(Throwable ex) {
            log.error("Disruptor dispatch executor start failed", ex);
        }

        /**
         * 处理关闭阶段异常。
         */
        @Override
        public void handleOnShutdownException(Throwable ex) {
            log.error("Disruptor dispatch executor shutdown failed", ex);
        }
    }
}

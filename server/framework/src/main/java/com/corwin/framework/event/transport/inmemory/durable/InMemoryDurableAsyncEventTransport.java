package com.corwin.framework.event.transport.inmemory.durable;

import com.corwin.framework.event.config.AsyncEventProperties;
import com.corwin.framework.event.context.ConsumeContextBinder;
import com.corwin.framework.event.context.ConsumeContextScope;
import com.corwin.framework.event.durable.store.*;
import com.corwin.framework.event.model.AsyncEventEnvelope;
import com.corwin.framework.event.serialize.EventSerializer;
import com.corwin.framework.event.subscription.SubscriptionDescriptor;
import com.corwin.framework.event.subscription.SubscriptionRegistry;
import com.corwin.framework.event.transport.AsyncEventTransport;
import com.corwin.framework.event.transport.inmemory.executor.InMemoryDispatchExecutor;
import com.corwin.framework.util.HighDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Durable in-memory transport backed by {@link com.corwin.framework.event.durable.store.PersistentEventStore}.
 * <p>
 * Core semantics:
 * <ol>
 *   <li>Publish persists event + delivery before enqueuing locally.</li>
 *   <li>Consume must claim first, ensuring each delivery is processed by only one node.</li>
 *   <li>Startup recovers incomplete deliveries; no periodic runtime recovery scan.</li>
 * </ol>
 *
 * @author Corwin 2026/4/12
 */
@Slf4j
public class InMemoryDurableAsyncEventTransport implements AsyncEventTransport {

    private static final Comparator<DelayedDeliveryTask> DELAYED_DELIVERY_COMPARATOR = Comparator.comparingLong(
            DelayedDeliveryTask::deliverAtMillis).thenComparingLong(DelayedDeliveryTask::sequence);

    private final AsyncEventProperties properties;
    private final PersistentEventStore eventStore;
    private final EventSerializer serializer;
    private final ConsumeContextBinder contextBinder;
    private final InMemoryDispatchExecutor executor;
    private final TransactionTemplate transactionTemplate;
    private final String nodeId;
    private final LinkedBlockingQueue<DeliveryTaskRef> readyQueue = new LinkedBlockingQueue<>();
    private final DelayQueue<DelayedDeliveryTask> delayQueue = new DelayQueue<>();
    private final AtomicLong sequenceGenerator = new AtomicLong(0L);
    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile SubscriptionRegistry registry;
    private volatile Map<String, SubscriptionDescriptor> subscriptionById = Map.of();
    private volatile Set<String> localConsumerGroups = Set.of();
    private volatile Thread readyWorker;
    private volatile Thread delayWorker;
    private volatile ScheduledExecutorService cleanerExecutor;

    public InMemoryDurableAsyncEventTransport(AsyncEventProperties properties, PersistentEventStore eventStore,
            EventSerializer serializer, ConsumeContextBinder contextBinder, InMemoryDispatchExecutor executor,
            PlatformTransactionManager transactionManager, String nodeId) {
        this.properties = Objects.requireNonNull(properties, "properties required");
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore required");
        this.serializer = Objects.requireNonNull(serializer, "serializer required");
        this.contextBinder = Objects.requireNonNull(contextBinder, "contextBinder required");
        this.executor = Objects.requireNonNull(executor, "executor required");
        Objects.requireNonNull(transactionManager, "transactionManager required");
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId required");
    }

    /**
     * 启动 durable transport。
     */
    @Override
    public void start(SubscriptionRegistry registry) {
        Objects.requireNonNull(registry, "registry required");
        if (!started.compareAndSet(false, true)) {
            return;
        }
        this.registry = registry;
        refreshLocalSubscriptions();
        eventStore.initializeSchema(properties.getDurable().getSchemaInitialize());
        loadStartupRecovery();
        executor.start();
        startWorkers();
        startCleanerIfEnabled();
        log.info("In-memory durable async-event transport started. nodeId={}, groups={}", nodeId, localConsumerGroups);
    }

    /**
     * 发布事件：事务内落库，事务后入队。
     */
    @Override
    public void publish(AsyncEventEnvelope<?> envelope) {
        Objects.requireNonNull(envelope, "envelope required");
        ensureStarted();
        List<SubscriptionDescriptor> subscriptions = resolveMatchedSubscriptions(envelope);
        if (subscriptions.isEmpty()) {
            return;
        }
        Runnable action = () -> persistAndEnqueue(envelope, subscriptions);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    /**
     * 停止 transport 并清理本地资源。
     */
    @Override
    public void shutdown() {
        if (!started.compareAndSet(true, false)) {
            return;
        }
        stopCleaner();
        stopWorkers();
        readyQueue.clear();
        delayQueue.clear();
        executor.shutdown();
        registry = null;
        subscriptionById = Map.of();
        localConsumerGroups = Set.of();
        log.info("In-memory durable async-event transport stopped. nodeId={}", nodeId);
    }

    /**
     * 刷新本节点订阅快照。
     */
    private void refreshLocalSubscriptions() {
        List<SubscriptionDescriptor> all = registry.getAll();
        this.subscriptionById = all.stream().collect(
                Collectors.toUnmodifiableMap(SubscriptionDescriptor::subscriptionId, value -> value,
                        (left, right) -> left));
        this.localConsumerGroups = all.stream().map(SubscriptionDescriptor::consumerGroup).collect(Collectors.toSet());
    }

    /**
     * 解析匹配 source 的订阅者集合。
     */
    private List<SubscriptionDescriptor> resolveMatchedSubscriptions(AsyncEventEnvelope<?> envelope) {
        List<SubscriptionDescriptor> subscriptions = registry.getByEventType(envelope.eventType());
        if (subscriptions.isEmpty()) {
            return List.of();
        }
        return subscriptions.stream().filter(subscription -> matchesSource(subscription, envelope.source())).toList();
    }

    /**
     * 持久化并入队。
     */
    private void persistAndEnqueue(AsyncEventEnvelope<?> envelope, List<SubscriptionDescriptor> subscriptions) {
        long now = HighDate.realTimestampMillis();
        String payloadBody = encodeEnvelope(envelope);
        PersistedEvent event = new PersistedEvent(envelope.eventId(), envelope.eventType(), envelope.source(),
                "base64-envelope-bytes", envelope.eventType(), payloadBody, envelope.occurredAtMillis(),
                envelope.deliverAtMillis(), EventStatus.ACTIVE, subscriptions.size(), 0, now, now, null, 0L);
        List<PersistedDelivery> deliveries = subscriptions.stream()
                .map(subscription -> new PersistedDelivery(UUID.randomUUID().toString(), envelope.eventId(),
                        subscription.subscriptionId(), subscription.consumerGroup(), envelope.deliverAtMillis(),
                        DeliveryStatus.PENDING, null, null, 0, null, now, now, null, 0L)).toList();
        transactionTemplate.executeWithoutResult(status -> eventStore.saveEvent(event, deliveries));
        for (PersistedDelivery delivery : deliveries) {
            enqueueTask(new DeliveryTaskRef(delivery.id(), delivery.eventId(), delivery.subscriberId(),
                    delivery.consumerGroup(), delivery.deliverAt(), delivery.version(),
                    sequenceGenerator.incrementAndGet()));
        }
    }

    /**
     * 入 ready 或 delay 队列。
     */
    private void enqueueTask(DeliveryTaskRef taskRef) {
        if (taskRef.deliverAtMillis() <= HighDate.realTimestampMillis()) {
            readyQueue.offer(taskRef);
            return;
        }
        delayQueue.offer(new DelayedDeliveryTask(taskRef));
    }

    /**
     * 启动 ready/delay worker。
     */
    private void startWorkers() {
        Thread ready = new Thread(this::runReadyWorker, "async-event-durable-ready-worker");
        ready.setDaemon(true);
        this.readyWorker = ready;
        ready.start();

        Thread delay = new Thread(this::runDelayWorker, "async-event-durable-delay-worker");
        delay.setDaemon(true);
        this.delayWorker = delay;
        delay.start();
    }

    /**
     * 停止 ready/delay worker。
     */
    private void stopWorkers() {
        Thread ready = readyWorker;
        readyWorker = null;
        if (ready != null) {
            ready.interrupt();
            joinQuietly(ready);
        }
        Thread delay = delayWorker;
        delayWorker = null;
        if (delay != null) {
            delay.interrupt();
            joinQuietly(delay);
        }
    }

    /**
     * ready worker：取任务并提交执行器处理。
     */
    private void runReadyWorker() {
        while (started.get()) {
            try {
                DeliveryTaskRef taskRef = readyQueue.take();
                executor.execute(() -> processDelivery(taskRef));
            } catch (InterruptedException ex) {
                if (!started.get()) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (Exception ex) {
                log.warn("Durable ready worker submit task failed", ex);
            }
        }
    }

    /**
     * delay worker：到期后搬运到 ready 队列。
     */
    private void runDelayWorker() {
        while (started.get()) {
            try {
                DelayedDeliveryTask delayedTask = delayQueue.take();
                if (!started.get()) {
                    return;
                }
                readyQueue.offer(delayedTask.taskRef());
            } catch (InterruptedException ex) {
                if (!started.get()) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (Exception ex) {
                log.warn("Durable delay worker move task failed", ex);
            }
        }
    }

    /**
     * 单条 delivery 执行。
     */
    private void processDelivery(DeliveryTaskRef taskRef) {
        long now = HighDate.realTimestampMillis();
        long claimTimeout = Math.max(1000L, properties.getDurable().getClaimTimeoutMs());
        long claimUntil = safeAdd(now, claimTimeout);
        boolean claimed = Boolean.TRUE.equals(transactionTemplate.execute(
                status -> eventStore.claimDelivery(taskRef.deliveryId(), taskRef.deliveryVersion(), nodeId, claimUntil,
                        now)));
        if (!claimed) {
            return;
        }
        long claimedVersion = taskRef.deliveryVersion() + 1;
        PersistedEvent event = eventStore.findEvent(taskRef.eventId()).orElse(null);
        if (event == null) {
            long updatedAt = HighDate.realTimestampMillis();
            transactionTemplate.executeWithoutResult(
                    status -> eventStore.markDeliveryCancelled(taskRef.deliveryId(), taskRef.eventId(), claimedVersion,
                            updatedAt, updatedAt));
            return;
        }
        SubscriptionDescriptor subscription = subscriptionById.get(taskRef.subscriberId());
        if (subscription == null) {
            long updatedAt = HighDate.realTimestampMillis();
            transactionTemplate.executeWithoutResult(
                    status -> eventStore.markDeliveryCancelled(taskRef.deliveryId(), taskRef.eventId(), claimedVersion,
                            updatedAt, updatedAt));
            return;
        }
        AsyncEventEnvelope<?> envelope = decodeEnvelope(event.payloadBody());
        try (ConsumeContextScope ignored = contextBinder.bind(envelope.ctxSnapshot())) {
            subscription.invoker().invoke(envelope);
            long completedAt = HighDate.realTimestampMillis();
            transactionTemplate.executeWithoutResult(
                    status -> eventStore.markDeliverySucceeded(taskRef.deliveryId(), taskRef.eventId(), claimedVersion,
                            completedAt, completedAt));
        } catch (Exception ex) {
            onConsumeFailed(taskRef, claimedVersion, ex);
        }
    }

    /**
     * 处理消费失败：标记失败并进行最小本地延时回队。
     */
    private void onConsumeFailed(DeliveryTaskRef taskRef, long claimedVersion, Exception ex) {
        long now = HighDate.realTimestampMillis();
        String errorMessage = ex.getClass().getSimpleName() + ": " + Objects.toString(ex.getMessage(), "");
        boolean marked = Boolean.TRUE.equals(transactionTemplate.execute(
                status -> eventStore.markDeliveryFailed(taskRef.deliveryId(), claimedVersion, errorMessage, now)));
        if (!marked) {
            return;
        }
        long backoffMs = Math.max(0L, properties.getDurable().getFailureBackoffMs());
        long requeueAt = safeAdd(now, backoffMs);
        enqueueTask(new DeliveryTaskRef(taskRef.deliveryId(), taskRef.eventId(), taskRef.subscriberId(),
                taskRef.consumerGroup(), requeueAt, claimedVersion + 1, sequenceGenerator.incrementAndGet()));
        log.warn(
                "Durable async-event consume failed and re-queued. eventId={}, deliveryId={}, subscriberId={}, nodeId={}",
                taskRef.eventId(), taskRef.deliveryId(), taskRef.subscriberId(), nodeId, ex);
    }

    /**
     * 启动恢复：只装载一次，不做运行时周期恢复扫描。
     */
    private void loadStartupRecovery() {
        if (localConsumerGroups.isEmpty()) {
            return;
        }
        int limit = Math.max(1, properties.getDurable().getStartupRecoveryBatchSize());
        long now = HighDate.realTimestampMillis();
        int offset = 0;
        int totalLoaded = 0;
        while (true) {
            List<PersistedDelivery> recoverable = eventStore.loadRecoverableDeliveries(localConsumerGroups, now, offset,
                    limit);
            if (recoverable.isEmpty()) {
                break;
            }
            for (PersistedDelivery delivery : recoverable) {
                enqueueTask(new DeliveryTaskRef(delivery.id(), delivery.eventId(), delivery.subscriberId(),
                        delivery.consumerGroup(), delivery.deliverAt(), delivery.version(),
                        sequenceGenerator.incrementAndGet()));
            }
            totalLoaded += recoverable.size();
            if (recoverable.size() < limit) {
                break;
            }
            offset += recoverable.size();
        }
        log.info("Durable startup recovery loaded {} deliveries", totalLoaded);
    }

    /**
     * 启动清理器。
     */
    private void startCleanerIfEnabled() {
        AsyncEventProperties.Durable.Cleanup cleanup = properties.getDurable().getCleanup();
        if (cleanup == null || !cleanup.isEnabled()) {
            return;
        }
        long intervalMs = Math.max(1000L, cleanup.getIntervalMs());
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "async-event-durable-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::runCleanupSafely, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        this.cleanerExecutor = scheduler;
    }

    /**
     * 停止清理器。
     */
    private void stopCleaner() {
        ScheduledExecutorService scheduler = cleanerExecutor;
        cleanerExecutor = null;
        if (scheduler == null) {
            return;
        }
        scheduler.shutdownNow();
    }

    /**
     * 执行一次清理并兜底异常。
     */
    private void runCleanupSafely() {
        try {
            AsyncEventProperties.Durable.Cleanup cleanup = properties.getDurable().getCleanup();
            if (cleanup == null || !cleanup.isEnabled()) {
                return;
            }
            long now = HighDate.realTimestampMillis();
            long deliveryBefore = now - Math.max(0L, cleanup.getDeliveryRetentionMs());
            long eventBefore = now - Math.max(0L, cleanup.getEventRetentionMs());
            int batchSize = Math.max(1, cleanup.getBatchSize());
            CleanupResult result = transactionTemplate.execute(
                    status -> eventStore.cleanupCompleted(deliveryBefore, eventBefore, batchSize));
            if (result != null && (result.deletedDeliveries() > 0 || result.deletedEvents() > 0)) {
                log.info("Durable cleanup deleted deliveries={}, events={}", result.deletedDeliveries(),
                        result.deletedEvents());
            }
        } catch (Exception ex) {
            log.warn("Durable cleanup failed", ex);
        }
    }

    /**
     * 判断 source 过滤是否匹配。
     */
    private boolean matchesSource(SubscriptionDescriptor subscription, String source) {
        List<String> sources = subscription.options().sources();
        if (sources.isEmpty()) {
            return true;
        }
        if (source == null || source.isBlank()) {
            return false;
        }
        return sources.contains(source.trim());
    }

    /**
     * 未启动时拒绝发布。
     */
    private void ensureStarted() {
        if (started.get()) {
            return;
        }
        throw new IllegalStateException("In-memory durable async-event transport is not started");
    }

    /**
     * 安全 long 相加，溢出时返回 Long.MAX_VALUE。
     */
    private long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ex) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * 以 Base64 形式持久化信封字节，避免 durable 存储错误依赖 UTF-8 文本假设。
     */
    private String encodeEnvelope(AsyncEventEnvelope<?> envelope) {
        return Base64.getEncoder().encodeToString(serializer.serialize(envelope));
    }

    /**
     * 从 Base64 文本恢复事件信封。
     */
    private AsyncEventEnvelope<?> decodeEnvelope(String encodedEnvelope) {
        Objects.requireNonNull(encodedEnvelope, "encodedEnvelope required");
        return serializer.deserialize(Base64.getDecoder().decode(encodedEnvelope));
    }

    /**
     * 等待线程短暂退出。
     */
    private void joinQuietly(Thread worker) {
        try {
            worker.join(1000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * DelayQueue 包装元素。
     */
    private final class DelayedDeliveryTask implements Delayed {

        private final DeliveryTaskRef taskRef;

        private DelayedDeliveryTask(DeliveryTaskRef taskRef) {
            this.taskRef = taskRef;
        }

        private DeliveryTaskRef taskRef() {
            return taskRef;
        }

        private long deliverAtMillis() {
            return taskRef.deliverAtMillis();
        }

        private long sequence() {
            return taskRef.sequence();
        }

        /**
         * 返回剩余延时。
         */
        @Override
        public long getDelay(TimeUnit unit) {
            long delayMillis = taskRef.deliverAtMillis() - HighDate.realTimestampMillis();
            return unit.convert(delayMillis, TimeUnit.MILLISECONDS);
        }

        /**
         * DelayQueue 比较规则。
         */
        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }
            if (other instanceof DelayedDeliveryTask task) {
                return DELAYED_DELIVERY_COMPARATOR.compare(this, task);
            }
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}

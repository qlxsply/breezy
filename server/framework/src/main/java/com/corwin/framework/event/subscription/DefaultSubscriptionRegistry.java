package com.corwin.framework.event.subscription;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 默认内存订阅注册表实现。
 * <p>
 * 设计要点：
 * <ul>
 *     <li>写入阶段加写锁，确保冲突检测与注册动作原子化。</li>
 *     <li>读取阶段使用并发容器 + 不可变拷贝，兼顾并发读取与结果安全。</li>
 *     <li>按配置执行 group 冲突 fail-fast，避免运行期出现隐式订阅覆盖。</li>
 * </ul>
 *
 * @author Corwin 2026/4/9
 */
public class DefaultSubscriptionRegistry implements SubscriptionRegistry {

    private final boolean failOnGroupConflict;
    private final CopyOnWriteArrayList<SubscriptionDescriptor> all = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SubscriptionDescriptor>> byEventType = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public DefaultSubscriptionRegistry(boolean failOnGroupConflict) {
        this.failOnGroupConflict = failOnGroupConflict;
    }

    /**
     * 注册订阅并执行重复 ID、group 冲突等校验。
     */
    @Override
    public void register(SubscriptionDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor required");
        lock.writeLock().lock();
        try {
            if (containsSubscriptionId(descriptor.subscriptionId())) {
                throw new IllegalStateException("Duplicate subscriptionId: " + descriptor.subscriptionId());
            }
            if (failOnGroupConflict && !descriptor.options().allowSharedGroup()) {
                assertNoGroupConflict(descriptor);
            }
            all.add(descriptor);
            byEventType.computeIfAbsent(descriptor.eventType(), key -> new CopyOnWriteArrayList<>()).add(descriptor);
            sortByOrder(byEventType.get(descriptor.eventType()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 查询指定事件类型的订阅列表。
     */
    @Override
    public List<SubscriptionDescriptor> getByEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return List.of();
        }
        List<SubscriptionDescriptor> found = byEventType.get(eventType);
        if (found == null || found.isEmpty()) {
            return List.of();
        }
        return List.copyOf(found);
    }

    /**
     * 获取全量订阅。
     */
    @Override
    public List<SubscriptionDescriptor> getAll() {
        return List.copyOf(all);
    }

    /**
     * 检查订阅 ID 是否重复。
     */
    private boolean containsSubscriptionId(String subscriptionId) {
        return all.stream().anyMatch(descriptor -> descriptor.subscriptionId().equals(subscriptionId));
    }

    /**
     * 校验同一事件类型下是否出现消费组冲突。
     */
    private void assertNoGroupConflict(SubscriptionDescriptor current) {
        for (SubscriptionDescriptor descriptor : byEventType.getOrDefault(current.eventType(), new CopyOnWriteArrayList<>())) {
            if (descriptor.consumerGroup().equals(current.consumerGroup())) {
                String message = String.format(
                        "Async event group conflict detected. eventType=%s, group=%s, existing=%s, incoming=%s",
                        current.eventType(), current.consumerGroup(), descriptor.subscriptionId(),
                        current.subscriptionId());
                throw new IllegalStateException(message);
            }
        }
    }

    /**
     * 按 order 升序排列订阅，保证消费顺序可预期。
     */
    private void sortByOrder(CopyOnWriteArrayList<SubscriptionDescriptor> descriptors) {
        if (descriptors == null || descriptors.size() <= 1) {
            return;
        }
        List<SubscriptionDescriptor> sorted = new ArrayList<>(descriptors);
        sorted.sort(Comparator.comparingInt(item -> item.options().order()));
        descriptors.clear();
        descriptors.addAll(sorted);
    }
}


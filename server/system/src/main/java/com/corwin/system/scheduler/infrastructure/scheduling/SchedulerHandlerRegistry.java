package com.corwin.system.scheduler.infrastructure.scheduling;

import com.corwin.system.scheduler.domain.model.HandlerKey;
import com.corwin.system.scheduler.domain.model.SchedulerJobHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 动态任务处理器注册表。
 *
 * @author Corwin 2026/4/15
 */
@Component
@RequiredArgsConstructor
public class SchedulerHandlerRegistry {

    private final List<SchedulerJobHandler<?>> handlers;
    private final Map<String, SchedulerJobHandler<?>> handlerMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        for (SchedulerJobHandler<?> handler : handlers) {
            HandlerKey key = Objects.requireNonNull(handler.key(), "handler key required");
            SchedulerJobHandler<?> existing = handlerMap.putIfAbsent(key.value(), handler);
            if (existing != null) {
                throw new IllegalStateException("duplicate scheduler handler key: " + key.value());
            }
        }
    }

    public Optional<SchedulerJobHandler<?>> get(HandlerKey key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlerMap.get(key.value()));
    }

    public Collection<SchedulerJobHandler<?>> listAll() {
        return List.copyOf(handlerMap.values());
    }
}

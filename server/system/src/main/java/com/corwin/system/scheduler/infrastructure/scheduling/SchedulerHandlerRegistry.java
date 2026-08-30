package com.corwin.system.scheduler.infrastructure.scheduling;

import com.corwin.system.scheduler.domain.model.HandlerKey;
import com.corwin.system.scheduler.domain.model.SchedulerJobHandler;
import jakarta.annotation.PostConstruct;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Registry for all {@link SchedulerJobHandler} beans, keyed by their {@link HandlerKey}.
 *
 * @author Corwin 2026/4/15
 */
@Component
@RequiredArgsConstructor
public class SchedulerHandlerRegistry {

  private final List<SchedulerJobHandler<?>> handlers;
  private final Map<String, SchedulerJobHandler<?>> handlerMap = new LinkedHashMap<>();

  /** Initializes the handler map, throwing on duplicate keys. */
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

  /** Looks up a handler by its key. */
  public Optional<SchedulerJobHandler<?>> get(HandlerKey key) {
    if (key == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(handlerMap.get(key.value()));
  }

  /**
   * Returns all registered handlers.
   *
   * @return collection of all registered handlers
   */
  public Collection<SchedulerJobHandler<?>> listAll() {
    return List.copyOf(handlerMap.values());
  }
}

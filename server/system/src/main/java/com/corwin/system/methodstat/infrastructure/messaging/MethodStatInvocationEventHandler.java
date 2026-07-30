package com.corwin.system.methodstat.infrastructure.messaging;

import com.corwin.framework.event.listener.AsyncEventListener;
import com.corwin.system.methodstat.application.service.MethodStatSwitchAppService;
import com.corwin.system.methodstat.domain.model.MethodStatAggregate;
import com.corwin.system.methodstat.domain.model.MethodStatInvocationEvent;
import com.corwin.system.methodstat.domain.repo.MethodStatAggregateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Event handler that processes {@link MethodStatInvocationEvent} asynchronously,
 * applying the invocation data to the relevant aggregate for statistics accumulation.
 * @author Corwin 2026/3/31
 */
@Slf4j
@Component
public class MethodStatInvocationEventHandler {

    private final MethodStatAggregateRepository aggregateRepository;
    private final MethodStatSwitchAppService switchAppService;

    public MethodStatInvocationEventHandler(MethodStatAggregateRepository aggregateRepository,
            MethodStatSwitchAppService switchAppService) {
        this.aggregateRepository = Objects.requireNonNull(aggregateRepository, "aggregateRepository required");
        this.switchAppService = Objects.requireNonNull(switchAppService, "switchAppService required");
    }

    /**
     * Handle the invocation event by applying it to the corresponding aggregate.
     * @param event the invocation event to process
     */
    @AsyncEventListener
    public void onEvent(MethodStatInvocationEvent event) {
        if (!switchAppService.isCollectEnabled(event.key())) {
            return;
        }
        MethodStatAggregate aggregate = aggregateRepository.getOrCreate(event.key());
        aggregate.apply(event);
    }

}

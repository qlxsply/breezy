package com.corwin.system.scheduler.infrastructure.scheduling;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.scheduler.domain.model.HandlerKey;
import com.corwin.system.scheduler.domain.model.JobPayload;
import com.corwin.system.scheduler.domain.model.SchedulerJobDefinition;
import com.corwin.system.scheduler.domain.model.SchedulerJobHandler;
import com.corwin.system.scheduler.domain.model.SchedulerJobRuntime;
import com.corwin.system.scheduler.domain.model.SchedulerTriggerType;
import com.corwin.system.scheduler.domain.model.ScheduleRule;
import com.corwin.system.scheduler.domain.repo.SchedulerJobDefinitionRepository;
import com.corwin.system.scheduler.domain.repo.SchedulerJobExecutionRepository;
import com.corwin.system.scheduler.domain.repo.SchedulerJobRuntimeRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 动态任务运行时注册器。
 *
 * @author Corwin 2026/4/15
 */
@Component
public class SchedulerRuntimeRegistrar {

    private final SchedulerJobDefinitionRepository definitionRepository;
    private final SchedulerJobRuntimeRepository runtimeRepository;
    private final SchedulerJobExecutionRepository executionRepository;
    private final SchedulerSerializer serializer;
    private final SchedulerHandlerRegistry handlerRegistry;
    private final ThreadPoolTaskScheduler schedulerJobTaskScheduler;
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public SchedulerRuntimeRegistrar(SchedulerJobDefinitionRepository definitionRepository,
            SchedulerJobRuntimeRepository runtimeRepository, SchedulerJobExecutionRepository executionRepository,
            SchedulerSerializer serializer, SchedulerHandlerRegistry handlerRegistry,
            @Qualifier("schedulerJobTaskScheduler") ThreadPoolTaskScheduler schedulerJobTaskScheduler) {
        this.definitionRepository = definitionRepository;
        this.runtimeRepository = runtimeRepository;
        this.executionRepository = executionRepository;
        this.serializer = serializer;
        this.handlerRegistry = handlerRegistry;
        this.schedulerJobTaskScheduler = schedulerJobTaskScheduler;
    }

    public void register(String jobId) {
        cancel(jobId);

        SchedulerJobDefinition definition = definitionRepository.findById(jobId)
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        if (definition.isDeleted() || !definition.isEnabled()) {
            return;
        }

        SchedulerJobHandler<?> handler = handlerRegistry.get(new HandlerKey(definition.getHandlerKey())).orElse(null);
        if (handler == null) {
            markInvalid(jobId, "scheduler handler not found: " + definition.getHandlerKey());
            return;
        }

        ScheduleRule scheduleRule;
        JobPayload payload;
        try {
            scheduleRule = serializer.deserializeScheduleRule(definition.getScheduleRuleBody(),
                    definition.getScheduleRuleType());
            payload = serializer.deserializePayload(definition.getPayloadBody(), definition.getPayloadType());
        } catch (Exception ex) {
            markInvalid(jobId, ex.getMessage());
            return;
        }

        Instant nextFireTime = ScheduleRuleTrigger.firstExecution(scheduleRule);
        SchedulerJobRuntime runtime = runtimeRepository.findById(jobId)
                .orElseGet(() -> SchedulerJobRuntime.create(jobId, null, null));
        runtime.markScheduled(nextFireTime);
        runtimeRepository.save(runtime);

        ReentrantLock lock = definition.isAllowConcurrent() ? null : locks.computeIfAbsent(jobId,
                key -> new ReentrantLock());
        SchedulerJobExecutionRunner runner = new SchedulerJobExecutionRunner(jobId, handler, payload, scheduleRule,
                SchedulerTriggerType.SCHEDULED, runtimeRepository, executionRepository, lock);
        ScheduledFuture<?> future = schedulerJobTaskScheduler.schedule(runner, new ScheduleRuleTrigger(scheduleRule));
        if (future != null) {
            futures.put(jobId, future);
        }
    }

    public void cancel(String jobId) {
        ScheduledFuture<?> future = futures.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void triggerNow(String jobId) {
        SchedulerJobDefinition definition = definitionRepository.findById(jobId).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));

        SchedulerJobHandler<?> handler = handlerRegistry.get(new HandlerKey(definition.getHandlerKey()))
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        ScheduleRule scheduleRule = serializer.deserializeScheduleRule(definition.getScheduleRuleBody(),
                definition.getScheduleRuleType());
        JobPayload payload = serializer.deserializePayload(definition.getPayloadBody(), definition.getPayloadType());
        ReentrantLock lock = definition.isAllowConcurrent() ? null : locks.computeIfAbsent(jobId,
                key -> new ReentrantLock());
        schedulerJobTaskScheduler.execute(
                new SchedulerJobExecutionRunner(jobId, handler, payload, scheduleRule, SchedulerTriggerType.MANUAL,
                        runtimeRepository, executionRepository, lock));
    }

    public void recoverAll() {
        for (SchedulerJobDefinition definition : definitionRepository.findAllByEnabledTrueAndDeletedFalse()) {
            register(definition.getJobId());
        }
    }

    private void markInvalid(String jobId, String message) {
        SchedulerJobRuntime runtime = runtimeRepository.findById(jobId)
                .orElseGet(() -> SchedulerJobRuntime.create(jobId, null, null));
        runtime.markInvalid(message);
        runtimeRepository.save(runtime);
    }
}

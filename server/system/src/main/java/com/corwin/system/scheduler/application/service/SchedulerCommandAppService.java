package com.corwin.system.scheduler.application.service;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.system.scheduler.domain.model.DynamicJobSpec;
import com.corwin.system.scheduler.domain.model.HandlerKey;
import com.corwin.system.scheduler.domain.model.JobPayload;
import com.corwin.system.scheduler.domain.model.ScheduleRule;
import com.corwin.system.scheduler.domain.model.SchedulerJobDefinition;
import com.corwin.system.scheduler.domain.model.SchedulerJobHandler;
import com.corwin.system.scheduler.domain.model.SchedulerJobRuntime;
import com.corwin.system.scheduler.domain.model.SchedulerJobStatus;
import com.corwin.system.scheduler.domain.repo.SchedulerJobDefinitionRepository;
import com.corwin.system.scheduler.domain.repo.SchedulerJobRuntimeRepository;
import com.corwin.system.scheduler.infrastructure.scheduling.ScheduleRuleTrigger;
import com.corwin.system.scheduler.infrastructure.scheduling.SchedulerRuntimeRegistrar;
import com.corwin.system.scheduler.infrastructure.scheduling.SchedulerSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * Application service for handling scheduler job command requests (upsert, pause, resume, cancel, delete, trigger).
 *
 * @author Corwin 2026/4/15
 */
@Service
public class SchedulerCommandAppService {

    private final SchedulerJobDefinitionRepository definitionRepository;
    private final SchedulerJobRuntimeRepository runtimeRepository;
    private final SchedulerSerializer serializer;
    private final SchedulerRuntimeRegistrar runtimeRegistrar;

    public SchedulerCommandAppService(SchedulerJobDefinitionRepository definitionRepository,
            SchedulerJobRuntimeRepository runtimeRepository, SchedulerSerializer serializer,
            SchedulerRuntimeRegistrar runtimeRegistrar) {
        this.definitionRepository = definitionRepository;
        this.runtimeRepository = runtimeRepository;
        this.serializer = serializer;
        this.runtimeRegistrar = runtimeRegistrar;
    }

    /**
     * Handles a job upsert request: creates or updates the job definition and runtime state.
     *
     * @param spec the job specification
     */
    @Transactional
    public void handleUpsert(DynamicJobSpec<?> spec) {
        BizAssert.notNull(spec, BaseError.MISSING_PARAMETER);
        BizAssert.notBlank(spec.jobId(), BaseError.MISSING_PARAMETER);
        BizAssert.notBlank(spec.name(), BaseError.MISSING_PARAMETER);
        BizAssert.notBlank(spec.namespace(), BaseError.MISSING_PARAMETER);
        BizAssert.notNull(spec.handler(), BaseError.MISSING_PARAMETER);
        BizAssert.notNull(spec.scheduleRule(), BaseError.MISSING_PARAMETER);

        SchedulerJobHandler<?> handler = spec.handler();
        HandlerKey handlerKey = handler.key();
        BizAssert.notNull(handlerKey, BaseError.MISSING_PARAMETER);

        JobPayload payload = (JobPayload) spec.payload();
        Class<?> payloadType = handler.payloadType();
        if (payload != null && payloadType != null && !payloadType.isInstance(payload)) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }

        ScheduleRule scheduleRule = spec.scheduleRule();
        byte[] scheduleRuleBody = serializer.serializeScheduleRule(scheduleRule);
        byte[] payloadBody = serializer.serializePayload(payload);
        String payloadTypeName = payload == null ? null : payload.getClass().getName();

        SchedulerJobDefinition definition = definitionRepository.findById(spec.jobId())
                .map(existing -> {
                    existing.updateEventJob(spec.namespace(), spec.name(), handlerKey, scheduleRule, scheduleRuleBody,
                            payloadTypeName, payloadBody, spec.enabled(), spec.allowConcurrent(), spec.remark());
                    return existing;
                })
                .orElseGet(() -> SchedulerJobDefinition.createEventJob(spec.jobId(), spec.namespace(), spec.name(),
                        handlerKey, scheduleRule, scheduleRuleBody, payloadTypeName, payloadBody, spec.enabled(),
                        spec.allowConcurrent(), spec.remark()));
        definitionRepository.save(definition);

        Instant firstExecution = spec.enabled() ? ScheduleRuleTrigger.firstExecution(scheduleRule) : null;
        SchedulerJobRuntime runtime = runtimeRepository.findById(spec.jobId())
                .orElseGet(() -> SchedulerJobRuntime.create(spec.jobId(),
                        spec.enabled() ? SchedulerJobStatus.SCHEDULED : SchedulerJobStatus.PAUSED, firstExecution));
        if (spec.enabled()) {
            runtime.markScheduled(firstExecution);
        } else {
            runtime.markPaused();
        }
        runtimeRepository.save(runtime);

        afterCommit(() -> {
            if (spec.enabled()) {
                runtimeRegistrar.register(spec.jobId());
            } else {
                runtimeRegistrar.cancel(spec.jobId());
            }
        });
    }

    /**
     * Handles a job pause request.
     *
     * @param jobId the job ID
     */
    @Transactional
    public void pauseRequested(String jobId) {
        SchedulerJobDefinition definition = requireDefinition(jobId);
        definition.markPaused();
        definitionRepository.save(definition);

        SchedulerJobRuntime runtime = runtimeRepository.findById(jobId)
                .orElseGet(() -> SchedulerJobRuntime.create(jobId, SchedulerJobStatus.PAUSED, null));
        runtime.markPaused();
        runtimeRepository.save(runtime);
        afterCommit(() -> runtimeRegistrar.cancel(jobId));
    }

    /**
     * Handles a job resume request.
     *
     * @param jobId the job ID
     */
    @Transactional
    public void resumeRequested(String jobId) {
        SchedulerJobDefinition definition = requireDefinition(jobId);
        BizAssert.state(!definition.isDeleted(), BaseError.NOT_FOUND);
        definition.markResumed();
        definitionRepository.save(definition);

        ScheduleRule rule = serializer.deserializeScheduleRule(definition.getScheduleRuleBody(),
                definition.getScheduleRuleType());
        Instant firstExecution = ScheduleRuleTrigger.firstExecution(rule);

        SchedulerJobRuntime runtime = runtimeRepository.findById(jobId)
                .orElseGet(() -> SchedulerJobRuntime.create(jobId, SchedulerJobStatus.SCHEDULED, firstExecution));
        runtime.markScheduled(firstExecution);
        runtimeRepository.save(runtime);
        afterCommit(() -> runtimeRegistrar.register(jobId));
    }

    /**
     * Handles a job cancel request.
     *
     * @param jobId the job ID
     */
    @Transactional
    public void cancelRequested(String jobId) {
        SchedulerJobDefinition definition = requireDefinition(jobId);
        definition.markCancelled();
        definitionRepository.save(definition);

        SchedulerJobRuntime runtime = runtimeRepository.findById(jobId)
                .orElseGet(() -> SchedulerJobRuntime.create(jobId, SchedulerJobStatus.CANCELLED, null));
        runtime.markCancelled();
        runtimeRepository.save(runtime);
        afterCommit(() -> runtimeRegistrar.cancel(jobId));
    }

    /**
     * Handles a job delete request (soft-delete).
     *
     * @param jobId the job ID
     */
    @Transactional
    public void deleteRequested(String jobId) {
        SchedulerJobDefinition definition = requireDefinition(jobId);
        definition.markDeleted();
        definitionRepository.save(definition);

        SchedulerJobRuntime runtime = runtimeRepository.findById(jobId)
                .orElseGet(() -> SchedulerJobRuntime.create(jobId, SchedulerJobStatus.CANCELLED, null));
        runtime.markCancelled();
        runtimeRepository.save(runtime);
        afterCommit(() -> runtimeRegistrar.cancel(jobId));
    }

    /**
     * Triggers a job execution immediately.
     *
     * @param jobId the job ID
     */
    public void triggerNow(String jobId) {
        requireDefinition(jobId);
        runtimeRegistrar.triggerNow(jobId);
    }

    private SchedulerJobDefinition requireDefinition(String jobId) {
        BizAssert.notBlank(jobId, BaseError.MISSING_PARAMETER);
        return definitionRepository.findById(jobId)
                .filter(definition -> !definition.isDeleted())
                .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
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
}

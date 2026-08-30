package com.corwin.system.scheduler.application.service;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.system.scheduler.application.view.SchedulerJobDetailView;
import com.corwin.system.scheduler.application.view.SchedulerJobExecutionView;
import com.corwin.system.scheduler.application.view.SchedulerJobView;
import com.corwin.system.scheduler.domain.model.SchedulerJobDefinition;
import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;
import com.corwin.system.scheduler.domain.model.SchedulerJobRuntime;
import com.corwin.system.scheduler.domain.repo.SchedulerJobDefinitionRepository;
import com.corwin.system.scheduler.domain.repo.SchedulerJobExecutionRepository;
import com.corwin.system.scheduler.domain.repo.SchedulerJobRuntimeRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Application service for querying scheduler job definitions, runtime states and execution history.
 *
 * @author Corwin 2026/4/15
 */
@Service
public class SchedulerQueryAppService {

  private final SchedulerJobDefinitionRepository definitionRepository;
  private final SchedulerJobRuntimeRepository runtimeRepository;
  private final SchedulerJobExecutionRepository executionRepository;

  public SchedulerQueryAppService(
      SchedulerJobDefinitionRepository definitionRepository,
      SchedulerJobRuntimeRepository runtimeRepository,
      SchedulerJobExecutionRepository executionRepository) {
    this.definitionRepository = definitionRepository;
    this.runtimeRepository = runtimeRepository;
    this.executionRepository = executionRepository;
  }

  /**
   * Lists all non-deleted jobs with their runtime status.
   *
   * @return list of job views
   */
  public List<SchedulerJobView> listJobs() {
    return definitionRepository.findAllByDeletedFalse().stream()
        .map(
            definition ->
                toView(definition, runtimeRepository.findById(definition.getJobId()).orElse(null)))
        .sorted(Comparator.comparing(SchedulerJobView::jobId))
        .toList();
  }

  /**
   * Gets detailed information for a single job.
   *
   * @param jobId the job ID
   * @return detail view
   */
  public SchedulerJobDetailView getJob(String jobId) {
    BizAssert.notBlank(jobId, BaseError.MISSING_PARAMETER);
    SchedulerJobDefinition definition =
        definitionRepository
            .findById(jobId)
            .filter(item -> !item.isDeleted())
            .orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    SchedulerJobRuntime runtime = runtimeRepository.findById(jobId).orElse(null);
    return toDetailView(definition, runtime);
  }

  /**
   * Paginated query for execution history of a specific job.
   *
   * @param jobId the job ID
   * @param spec pagination specification
   * @return paginated execution views
   */
  public PageData<SchedulerJobExecutionView> pageExecutions(String jobId, PageSpec spec) {
    BizAssert.notBlank(jobId, BaseError.MISSING_PARAMETER);
    PageData<SchedulerJobExecution> pageData = executionRepository.pageByJobId(jobId, spec);
    return PageData.of(
        pageData.pageNo(),
        pageData.pageSize(),
        pageData.totalElements(),
        pageData.elements().stream().map(this::toExecutionView).toList());
  }

  private SchedulerJobView toView(SchedulerJobDefinition definition, SchedulerJobRuntime runtime) {
    return new SchedulerJobView(
        definition.getJobId(),
        definition.getNamespace(),
        definition.getName(),
        definition.getJobType(),
        definition.getHandlerKey(),
        definition.getScheduleRuleType(),
        definition.isEnabled(),
        definition.isAllowConcurrent(),
        definition.getRemark(),
        runtime == null ? null : runtime.getStatus(),
        runtime == null ? null : runtime.getNextFireTime(),
        runtime == null ? null : runtime.getLastFireTime(),
        runtime == null ? null : runtime.getLastSuccessTime(),
        runtime == null ? null : runtime.getLastFailureTime(),
        runtime == null ? null : runtime.getLastErrorMessage(),
        definition.getUpdatedAt());
  }

  private SchedulerJobDetailView toDetailView(
      SchedulerJobDefinition definition, SchedulerJobRuntime runtime) {
    return new SchedulerJobDetailView(
        definition.getJobId(),
        definition.getNamespace(),
        definition.getName(),
        definition.getSource(),
        definition.getJobType(),
        definition.getHandlerKey(),
        definition.getScheduleRuleType(),
        definition.getPayloadType(),
        definition.isEnabled(),
        definition.isDeleted(),
        definition.isAllowConcurrent(),
        definition.getVersionNo(),
        definition.getRemark(),
        runtime == null ? null : runtime.getStatus(),
        runtime == null ? null : runtime.getNextFireTime(),
        runtime == null ? null : runtime.getLastFireTime(),
        runtime == null ? null : runtime.getLastSuccessTime(),
        runtime == null ? null : runtime.getLastFailureTime(),
        runtime == null ? null : runtime.getConsecutiveFailures(),
        runtime == null ? null : runtime.getLastErrorMessage(),
        runtime == null ? null : runtime.getLastDurationMs(),
        runtime == null ? null : runtime.getCurrentExecutionId(),
        definition.getCreatedAt(),
        definition.getUpdatedAt());
  }

  private SchedulerJobExecutionView toExecutionView(SchedulerJobExecution execution) {
    return new SchedulerJobExecutionView(
        execution.getExecutionId(),
        execution.getJobId(),
        execution.getScheduledTime(),
        execution.getStartTime(),
        execution.getEndTime(),
        execution.getResult(),
        execution.getDurationMs(),
        execution.getTriggerType(),
        execution.getResultCode(),
        execution.getResultMessage(),
        execution.getErrorMessage());
  }
}

package com.corwin.system.audit.application.service;

import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.system.audit.application.command.AuditRecordCommand;
import com.corwin.system.audit.domain.model.AuditLevel;
import com.corwin.system.audit.domain.model.AuditLog;
import com.corwin.system.audit.domain.repo.AuditLogPageQuery;
import com.corwin.system.audit.domain.repo.AuditLogRepository;
import com.corwin.system.config.application.config.SystemConfigKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.Executor;

/**
 * Application service for audit log operations.
 * Provides synchronous and asynchronous recording of audit entries,
 * retrieval by ID, and paginated querying with dynamic filters.
 *
 * @author Corwin 2026/4/19
 */
@Slf4j
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final Executor taskExecutor;

    public AuditLogService(AuditLogRepository auditLogRepository, @Qualifier("taskExecutor") Executor taskExecutor) {
        this.auditLogRepository = auditLogRepository;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Records an audit log entry synchronously or asynchronously based on configuration.
     * Builds an {@link AuditLog} domain object from the command and persists it.
     * If async mode is enabled, the save is submitted to the task executor;
     * on submission failure it falls back to synchronous saving.
     *
     * @param command the audit record command carrying all log data
     */
    public void record(AuditRecordCommand command) {
        if (command == null) {
            return;
        }

        AuditLog auditLog = new AuditLog(command.traceId(), command.requestId(), command.operatorUserId(),
                StrUtil.trimToNull(command.operatorUsername()), StrUtil.trimToNull(command.operatorUserType()),
                StrUtil.trimToNull(command.applicationCode()), command.protocol(), command.httpMethod(),
                StrUtil.trimToNull(command.pathPattern()), StrUtil.trimToNull(command.requestUri()),
                StrUtil.trimToNull(command.permissionCodes()), StrUtil.trimToNull(command.auditResource()),
                StrUtil.trimToNull(command.auditAction()), StrUtil.trimToNull(command.auditDescription()),
                command.auditLevel(), StrUtil.trimToNull(command.requestIp()), StrUtil.trimToNull(command.userAgent()),
                StrUtil.trimToNull(command.requestParamSummary()), StrUtil.trimToNull(command.requestBodySummary()),
                StrUtil.trimToNull(command.responseSummary()), command.success(),
                StrUtil.trimToNull(command.errorCode()), StrUtil.trimToNull(command.errorMessage()),
                command.startedAt(), command.endedAt(), command.durationMs(),
                command.createdAt() == null ? HighDate.mockInstant() : command.createdAt());

        if (!ConfigRegistry.booleanV(SystemConfigKeys.AUDIT_ASYNC_ENABLED)) {
            saveSafely(auditLog);
            return;
        }

        try {
            taskExecutor.execute(() -> saveSafely(auditLog));
        } catch (RuntimeException ex) {
            log.warn("Submit audit log task failed, fallback to sync save: {}", ex.getMessage());
            saveSafely(auditLog);
        }
    }

    /**
     * Retrieves an audit log by its ID.
     *
     * @param id the audit log primary key
     * @return the matching {@link AuditLog} entity
     * @throws com.corwin.framework.error.BizException if the id is null or the record is not found
     */
    public AuditLog get(Long id) {
        BizAssert.notNull(id, BaseError.INVALID_PARAMETER);
        AuditLog auditLog = auditLogRepository.findById(id).orElse(null);
        BizAssert.notNull(auditLog, BaseError.NOT_FOUND);
        return auditLog;
    }

    /**
     * Performs a paginated query of audit logs with dynamic filters.
     *
     * @param traceId          optional trace ID filter
     * @param operatorUserId   optional operator user ID filter
     * @param operatorUsername optional operator username filter
     * @param applicationCode  optional application code filter
     * @param requestUri       optional request URI filter
     * @param auditResource    optional audit resource filter
     * @param auditAction      optional audit action filter
     * @param auditLevel       optional severity level filter
     * @param success          optional success status filter
     * @param startAt          optional start of the time range
     * @param endAt            optional end of the time range
     * @param spec             pagination and sort specification
     * @return a page of matching {@link AuditLog} entities
     */
    public PageData<AuditLog> page(String traceId, Long operatorUserId, String operatorUsername, String applicationCode,
            String requestUri, String auditResource, String auditAction, AuditLevel auditLevel, Boolean success,
            Instant startAt, Instant endAt, PageSpec spec) {
        AuditLogPageQuery query = new AuditLogPageQuery(StrUtil.trimToNull(traceId), operatorUserId,
                StrUtil.trimToNull(operatorUsername), StrUtil.trimToNull(applicationCode),
                StrUtil.trimToNull(requestUri), StrUtil.trimToNull(auditResource), StrUtil.trimToNull(auditAction),
                auditLevel, success, startAt, endAt);
        return auditLogRepository.pageByQuery(query, PageSpecSorts.apply(spec));
    }

    private void saveSafely(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (RuntimeException ex) {
            log.warn("Failed to save audit log: {}", ex.getMessage(), ex);
        }
    }

}

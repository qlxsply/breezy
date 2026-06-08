package com.corwin.system.audit.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XTableQuery;
import com.corwin.system.audit.domain.model.AuditLog;
import com.corwin.system.audit.domain.repo.AuditLogPageQuery;
import com.corwin.system.audit.domain.repo.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Corwin 2026/4/19
 */
@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryJpaAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

    @Override
    public <S extends AuditLog> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends AuditLog> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<AuditLog> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(AuditLog entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<AuditLog> pageByQuery(AuditLogPageQuery query, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;

        String traceId = query == null ? null : StrUtil.trimToNull(query.traceId());
        Long operatorUserId = query == null ? null : query.operatorUserId();
        String operatorUsername = query == null ? null : StrUtil.trimToNull(query.operatorUsername());
        String applicationCode = query == null ? null : StrUtil.trimToNull(query.applicationCode());
        String requestUri = query == null ? null : StrUtil.trimToNull(query.requestUri());
        String auditResource = query == null ? null : StrUtil.trimToNull(query.auditResource());
        String auditAction = query == null ? null : StrUtil.trimToNull(query.auditAction());

        XTableQuery<AuditLog, AuditLog> dynamicQuery = xSql.using(dataSource).table(AuditLog.class, AuditLog.class)
                .likeIf(traceId != null, AuditLog::getTraceId, traceId)
                .eqIf(operatorUserId != null, AuditLog::getOperatorUserId, operatorUserId)
                .likeIf(operatorUsername != null, AuditLog::getOperatorUsername, operatorUsername)
                .eqIf(applicationCode != null, AuditLog::getApplicationCode, applicationCode)
                .likeIf(requestUri != null, AuditLog::getRequestUri, requestUri)
                .likeIf(auditResource != null, AuditLog::getAuditResource, auditResource)
                .likeIf(auditAction != null, AuditLog::getAuditAction, auditAction)
                .eqIf(query != null && query.auditLevel() != null, AuditLog::getAuditLevel, query.auditLevel())
                .eqIf(query != null && query.success() != null, AuditLog::getSuccess, query.success())
                .geIf(query != null && query.startAt() != null, AuditLog::getCreatedAt, query.startAt())
                .ltIf(query != null && query.endAt() != null, AuditLog::getCreatedAt, query.endAt());
        applySort(dynamicQuery, resolvedSpec);
        return dynamicQuery.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    private void applySort(XTableQuery<AuditLog, AuditLog> query, PageSpec spec) {
        if (spec == null || spec.sorts().isEmpty()) {
            return;
        }
        for (SortSpec sort : spec.sorts()) {
            if (sort == null) {
                continue;
            }
            String field = normalizeSortField(sort.field());
            if (field == null) {
                continue;
            }
            query.orderBy(field, toXSortDirection(sort.direction()));
        }
    }

    private String normalizeSortField(String field) {
        String normalized = StrUtil.trimToNull(field);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "ID" -> "id";
            case "TRACE_ID", "TRACEID" -> "traceId";
            case "REQUEST_ID", "REQUESTID" -> "requestId";
            case "OPERATOR_USER_ID", "OPERATORUSERID", "USER_ID", "USERID" -> "operatorUserId";
            case "OPERATOR_USERNAME", "OPERATORUSERNAME", "USERNAME" -> "operatorUsername";
            case "APPLICATION_CODE", "APPLICATIONCODE" -> "applicationCode";
            case "PROTOCOL" -> "protocol";
            case "HTTP_METHOD", "HTTPMETHOD", "METHOD" -> "httpMethod";
            case "PATH_PATTERN", "PATHPATTERN" -> "pathPattern";
            case "REQUEST_URI", "REQUESTURI" -> "requestUri";
            case "AUDIT_RESOURCE", "AUDITRESOURCE", "RESOURCE" -> "auditResource";
            case "AUDIT_ACTION", "AUDITACTION", "ACTION" -> "auditAction";
            case "AUDIT_LEVEL", "AUDITLEVEL", "LEVEL" -> "auditLevel";
            case "SUCCESS" -> "success";
            case "REQUEST_IP", "REQUESTIP", "LOGIN_IP", "LOGINIP" -> "requestIp";
            case "DURATION_MS", "DURATIONMS", "DURATION" -> "durationMs";
            case "STARTED_AT", "STARTEDAT" -> "startedAt";
            case "ENDED_AT", "ENDEDAT" -> "endedAt";
            case "CREATED_AT", "CREATEDAT" -> "createdAt";
            default -> normalized;
        };
    }

    private XSortDirection toXSortDirection(SortDirection direction) {
        if (direction == SortDirection.DESC) {
            return XSortDirection.DESC;
        }
        return XSortDirection.ASC;
    }
}

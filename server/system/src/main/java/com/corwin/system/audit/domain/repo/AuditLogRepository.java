package com.corwin.system.audit.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import com.corwin.system.audit.domain.model.AuditLog;

/**
 * Repository interface for {@link AuditLog} domain entities. Extends base CRUD and dynamic page
 * query capabilities for audit log persistence.
 *
 * @author Corwin 2026/4/19
 */
public interface AuditLogRepository
    extends DomainRepository<AuditLog, Long>,
        DynamicPageQueryRepository<AuditLog, AuditLogPageQuery> {}

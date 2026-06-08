package com.corwin.system.audit.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import com.corwin.system.audit.domain.model.AuditLog;

/**
 * @author Corwin 2026/4/19
 */
public interface AuditLogRepository extends DomainRepository<AuditLog, Long>, DynamicPageQueryRepository<AuditLog, AuditLogPageQuery> {
}

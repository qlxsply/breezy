package com.corwin.system.audit.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.audit.domain.model.AuditLog;
import com.corwin.system.audit.domain.repo.AuditLogPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for paginated audit log queries.
 * Provides dynamic query execution for the audit log list page
 * using the {@link AuditLogPageQuery} filter and {@link PageSpec} sorting/pagination.
 *
 * @author Corwin 2026/7/28
 */
@Mapper
public interface AuditLogMybatisMapper {

    /**
     * Executes a paginated query for audit logs with dynamic filters.
     *
     * @param query the query filter parameters
     * @param spec  the pagination and sort specification
     * @return a page of matching {@link AuditLog} entities
     */
    PageData<AuditLog> pageByQuery(@Param("query") AuditLogPageQuery query, @Param("spec") PageSpec spec);
}

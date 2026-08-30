package com.corwin.system.auth.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import com.corwin.system.auth.domain.model.LoginEvent;

/**
 * Repository interface for managing {@link LoginEvent} entities with support for dynamic page
 * queries via {@link LoginLogPageQuery}.
 *
 * @author Corwin 2026/1/23
 */
public interface LoginLogRepository
    extends DomainRepository<LoginEvent, Long>,
        DynamicPageQueryRepository<LoginEvent, LoginLogPageQuery> {}

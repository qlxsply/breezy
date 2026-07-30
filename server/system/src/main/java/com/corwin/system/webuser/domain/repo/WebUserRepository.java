package com.corwin.system.webuser.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserStatus;

/**
 * Domain repository for {@link WebUser} aggregate root.
 * Extends basic CRUD with paginated and filtered queries.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserRepository extends DomainRepository<WebUser, Long> {

    /**
     * Retrieve all users with pagination.
     *
     * @param spec pagination specification
     * @return paginated result
     */
    PageData<WebUser> findAll(PageSpec spec);

    /**
     * Paginated query with optional keyword and status filter.
     *
     * @param keyword optional search keyword for display/nickname
     * @param status  optional status filter
     * @param spec    pagination specification
     * @return paginated result
     */
    PageData<WebUser> page(String keyword, WebUserStatus status, PageSpec spec);

}

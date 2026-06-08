package com.corwin.system.webuser.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserStatus;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserRepository extends DomainRepository<WebUser, Long> {

    PageData<WebUser> findAll(PageSpec spec);

    PageData<WebUser> findByStatus(WebUserStatus status, PageSpec spec);

    PageData<WebUser> findByKeyword(String keyword, PageSpec spec);

    PageData<WebUser> findByStatusAndKeyword(WebUserStatus status, String keyword, PageSpec spec);
}

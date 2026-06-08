package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.ApiPermission;

/**
 * @author Corwin 2026/4/24
 */
public interface ApiPermissionRepository extends DomainRepository<ApiPermission, Long> {

    void deleteByApiId(Long apiId);

}

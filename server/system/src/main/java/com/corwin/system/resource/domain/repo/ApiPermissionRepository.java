package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.ApiPermission;

/**
 * Repository interface for {@link ApiPermission} entity operations.
 *
 * @author Corwin 2026/4/24
 */
public interface ApiPermissionRepository extends DomainRepository<ApiPermission, Long> {

    /**
     * Deletes all API-permission bindings for the given API ID.
     *
     * @param apiId the API ID
     */
    void deleteByApiId(Long apiId);

}

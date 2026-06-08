package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.FunctionPermission;

import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/5/7
 */
public interface FunctionPermissionRepository extends DomainRepository<FunctionPermission, Long> {

    List<FunctionPermission> findAll();

    List<FunctionPermission> findByFunctionIdIn(Collection<Long> functionIds);
}

package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.Permission;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/4/19
 */
public interface PermissionRepository extends DomainRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findAll();

    List<Permission> findAllById(Iterable<Long> ids);
}

package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.Resource;

import java.util.Collection;
import java.util.List;

/**
 * @author Corwin 2026/6/29
 */
public interface ResourceRepository extends DomainRepository<Resource, Long> {

    List<Resource> findAll();

    List<Resource> findAllById(Collection<Long> ids);
}

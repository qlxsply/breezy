package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.Api;

import java.util.List;

/**
 * @author Corwin 2026/1/23
 */
public interface ApiRepository extends DomainRepository<Api, Long> {

    List<Api> findByIdIn(List<Long> ids);

    List<Api> findAll();

    PageData<Api> page(String keyword, Boolean enabled, PageSpec spec);

}

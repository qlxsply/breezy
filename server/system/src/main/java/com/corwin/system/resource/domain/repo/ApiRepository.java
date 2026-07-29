package com.corwin.system.resource.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/23
 */
public interface ApiRepository extends DomainRepository<Api, Long> {

    List<Api> findByIdIn(List<Long> ids);

    List<Api> findAll();

    PageData<Api> page(ApiPageQuery query, PageSpec spec);

    Optional<Api> findFirstByProtocolAndHttpMethodAndPathPattern(ApiProtocol protocol, ApiMethod httpMethod,
            String pathPattern);

}

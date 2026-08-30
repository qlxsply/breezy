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
 * Repository interface for {@link Api} entity operations.
 *
 * @author Corwin 2026/1/23
 */
public interface ApiRepository extends DomainRepository<Api, Long> {

  /**
   * Returns APIs whose IDs are in the given list.
   *
   * @param ids the list of API IDs
   * @return the matching APIs
   */
  List<Api> findByIdIn(List<Long> ids);

  /**
   * Returns all APIs.
   *
   * @return the list of all APIs
   */
  List<Api> findAll();

  /**
   * Paginated query for APIs with dynamic filtering.
   *
   * @param query the query filter criteria
   * @param spec the page specification
   * @return the paginated API data
   */
  PageData<Api> page(ApiPageQuery query, PageSpec spec);

  /**
   * Finds the first API matching the given protocol, HTTP method, and path pattern.
   *
   * @param protocol the API protocol
   * @param httpMethod the HTTP method
   * @param pathPattern the request path pattern
   * @return an Optional containing the matching API if found
   */
  Optional<Api> findFirstByProtocolAndHttpMethodAndPathPattern(
      ApiProtocol protocol, ApiMethod httpMethod, String pathPattern);
}

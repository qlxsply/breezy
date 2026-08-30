package com.corwin.system.resource.application.service;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.sort.PageSpecSorts;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.repo.ApiPageQuery;
import com.corwin.system.resource.domain.repo.ApiRepository;
import com.corwin.system.resource.infrastructure.sort.ApiSortOptionsParser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for admin API metadata management.
 *
 * <p>Provides use-case orchestration for listing, paginating, enabling/disabling APIs, and updating
 * sort options. Clears the API permission cache on state changes.
 *
 * @author Corwin 2026/1/23
 */
@Service
@RequiredArgsConstructor
public class ApiAdminService {

  private final ApiRepository apiRepository;
  private final ApiPermissionCache apiPermissionCache;

  /**
   * Lists all registered API metadata.
   *
   * @return the list of all APIs
   */
  public List<Api> listAll() {
    return apiRepository.findAll();
  }

  /**
   * Paginated query for APIs with dynamic filtering and sorting support.
   *
   * @param query the query filter criteria
   * @param spec the page specification
   * @return the paginated API data
   */
  public PageData<Api> page(ApiPageQuery query, PageSpec spec) {
    return apiRepository.page(query, PageSpecSorts.apply(spec));
  }

  /**
   * Retrieves an API by its ID.
   *
   * @param id the API ID
   * @return the API entity
   * @throws BizException if not found
   */
  public Api get(Long id) {
    return apiRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
  }

  /**
   * Publishes (enables) an API and clears the permission cache.
   *
   * @param id the API ID
   * @return the updated API entity
   */
  @Transactional
  public Api publish(Long id) {
    Api api = get(id);
    api.enable();
    Api saved = apiRepository.save(api);
    apiPermissionCache.clearAll();
    return saved;
  }

  /**
   * Disables an API and clears the permission cache.
   *
   * @param id the API ID
   * @return the updated API entity
   */
  @Transactional
  public Api disable(Long id) {
    Api api = get(id);
    api.disable();
    Api saved = apiRepository.save(api);
    apiPermissionCache.clearAll();
    return saved;
  }

  /**
   * Validates and updates the sort options JSON configuration for an API.
   *
   * @param id the API ID
   * @param sortOptionsJson the sort options JSON string
   * @return the updated API entity
   */
  @Transactional
  public Api updateSortOptions(Long id, String sortOptionsJson) {
    Api api = get(id);
    String normalized = ApiSortOptionsParser.normalize(sortOptionsJson);
    api.updateSortOptions(normalized);
    return apiRepository.save(api);
  }
}

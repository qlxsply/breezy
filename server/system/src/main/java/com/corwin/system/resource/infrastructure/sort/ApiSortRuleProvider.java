package com.corwin.system.resource.infrastructure.sort;

import com.corwin.framework.web.sort.SortRule;
import com.corwin.framework.web.sort.SortRuleProvider;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;
import com.corwin.system.resource.domain.repo.ApiRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link SortRuleProvider} that resolves sort rules from API sort options JSON.
 *
 * <p>Matches the current request to an API record by protocol, HTTP method, and path pattern, then
 * parses the {@code sort_options_json} column to produce a {@link SortRule}.
 *
 * @author Corwin 2026/7/29
 */
@Component
@RequiredArgsConstructor
public class ApiSortRuleProvider implements SortRuleProvider {

  private final ApiRepository apiRepository;

  @Override
  public SortRule getRule(HttpServletRequest request, HandlerMethod handlerMethod) {
    ApiMethod method = resolveMethod(request.getMethod());
    String pathPattern = resolvePathPattern(request);
    if (method == null || pathPattern == null) {
      return SortRule.disabled();
    }
    return apiRepository
        .findFirstByProtocolAndHttpMethodAndPathPattern(ApiProtocol.HTTP, method, pathPattern)
        .map(Api::getSortOptionsJson)
        .map(ApiSortOptionsParser::parse)
        .orElseGet(SortRule::disabled);
  }

  /**
   * Resolves the HTTP method string to an {@link ApiMethod} enum.
   *
   * @param raw the raw HTTP method string
   * @return the ApiMethod enum, or null if unresolvable
   */
  private ApiMethod resolveMethod(String raw) {
    try {
      return ApiMethod.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }

  /**
   * Resolves the best-matching path pattern from the current request.
   *
   * @param request the HTTP request
   * @return the path pattern string, or null if not available
   */
  private String resolvePathPattern(HttpServletRequest request) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    if (pattern == null) {
      return null;
    }
    return String.valueOf(pattern);
  }
}

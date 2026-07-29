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
 * 基于 sys_api.sort_options_json 的排序规则提供者。
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
        return apiRepository.findFirstByProtocolAndHttpMethodAndPathPattern(ApiProtocol.HTTP, method, pathPattern)
                .map(Api::getSortOptionsJson)
                .map(ApiSortOptionsParser::parse)
                .orElseGet(SortRule::disabled);
    }

    private ApiMethod resolveMethod(String raw) {
        try {
            return ApiMethod.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private String resolvePathPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return null;
        }
        return String.valueOf(pattern);
    }
}

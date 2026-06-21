package com.corwin.system.resource.application.service;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.repo.ApiPageQuery;
import com.corwin.system.resource.domain.repo.ApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Corwin 2026/1/23
 */
@Service
@RequiredArgsConstructor
public class ApiAdminService {

    private final ApiRepository apiRepository;
    private final ApiPermissionCache apiPermissionCache;

    public List<Api> listAll() {
        return apiRepository.findAll();
    }

    public PageData<Api> page(ApiPageQuery query, PageSpec spec) {
        return apiRepository.page(query, spec);
    }

    public Api get(Long id) {
        return apiRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    @Transactional
    public Api publish(Long id) {
        Api api = get(id);
        api.enable();
        Api saved = apiRepository.save(api);
        apiPermissionCache.clearAll();
        return saved;
    }

    @Transactional
    public Api disable(Long id) {
        Api api = get(id);
        api.disable();
        Api saved = apiRepository.save(api);
        apiPermissionCache.clearAll();
        return saved;
    }
}

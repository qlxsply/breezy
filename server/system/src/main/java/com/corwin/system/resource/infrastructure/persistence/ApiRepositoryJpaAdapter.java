package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.framework.util.StrUtil;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.model.ApiMethod;
import com.corwin.system.resource.domain.model.ApiProtocol;
import com.corwin.system.resource.domain.repo.ApiPageQuery;
import com.corwin.system.resource.domain.repo.ApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class ApiRepositoryJpaAdapter implements ApiRepository {

    private final ApiJpaRepository repo;
    private final ApiMybatisMapper mybatisMapper;

    @Override
    public <S extends Api> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Api> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Api> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Api entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<Api> findByIdIn(List<Long> ids) {
        return repo.findByIdIn(ids);
    }

    @Override
    public List<Api> findAll() {
        return repo.findAll();
    }

    @Override
    public PageData<Api> page(ApiPageQuery query, PageSpec spec) {
        return mybatisMapper.page(normalizeQuery(query), spec);
    }

    @Override
    public Optional<Api> findFirstByProtocolAndHttpMethodAndPathPattern(ApiProtocol protocol, ApiMethod httpMethod,
            String pathPattern) {
        return repo.findFirstByProtocolAndHttpMethodAndPathPattern(protocol, httpMethod, pathPattern);
    }

    private ApiPageQuery normalizeQuery(ApiPageQuery query) {
        if (query == null) {
            return null;
        }
        return new ApiPageQuery(StrUtil.trimToNull(query.module()),
                LikePatternUtils.toContainsPattern(query.pathPattern()),
                LikePatternUtils.toContainsPattern(query.handlerClass()),
                LikePatternUtils.toContainsPattern(query.handlerMethod()), query.permissionDeclared(),
                query.accessType(), query.userType(), query.auditDeclared(), query.enabled());
    }
}

package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XTableQuery;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.repo.ApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class ApiRepositoryJpaAdapter implements ApiRepository {

    private final ApiJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

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
    public PageData<Api> page(String keyword, Boolean enabled, PageSpec spec) {
        keyword = StrUtil.trimToNull(keyword);
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;

        XTableQuery<Api, Api> dynamicQuery = xSql.using(dataSource).table(Api.class, Api.class)
                .likeIf(StrUtil.isNotBlank(keyword), Api::getPathPattern, "%" + keyword + "%")
                .eqIf(enabled != null, Api::getEnabled, enabled).applySort(resolvedSpec);
        return dynamicQuery.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

}

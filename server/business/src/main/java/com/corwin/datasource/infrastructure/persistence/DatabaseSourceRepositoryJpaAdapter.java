package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.datasource.domain.repo.DatabaseSourcePageQuery;
import com.corwin.datasource.domain.repo.DatabaseSourceRepository;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/11
 */
@Repository
@RequiredArgsConstructor
public class DatabaseSourceRepositoryJpaAdapter implements DatabaseSourceRepository {

    private final DatabaseSourceJpaRepository repo;
    private final DatabaseSourceMybatisMapper mybatisMapper;

    @Override
    public <S extends DatabaseSource> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends DatabaseSource> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<DatabaseSource> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(DatabaseSource entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<DatabaseSource> findByName(String name) {
        return repo.findByName(name);
    }

    @Override
    public Optional<DatabaseSource> findByDbTypeAndJdbcUrlAndUsername(DatabaseType dbType, String jdbcUrl,
            String username) {
        return repo.findByDbTypeAndJdbcUrlAndUsername(dbType, jdbcUrl, username);
    }

    @Override
    public Optional<DatabaseSource> findByAppDsKey(String appDsKey) {
        return repo.findByAppDsKey(appDsKey);
    }

    @Override
    public List<DatabaseSource> findAll() {
        return repo.findAll();
    }

    @Override
    public PageData<DatabaseSource> pageByQuery(DatabaseSourcePageQuery query, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;
        return mybatisMapper.pageByQuery(normalizeQuery(query), resolvedSpec);
    }

    private DatabaseSourcePageQuery normalizeQuery(DatabaseSourcePageQuery query) {
        if (query == null) {
            return null;
        }
        return new DatabaseSourcePageQuery(query.dbType(), LikePatternUtils.toContainsPattern(query.nameLike()));
    }
}

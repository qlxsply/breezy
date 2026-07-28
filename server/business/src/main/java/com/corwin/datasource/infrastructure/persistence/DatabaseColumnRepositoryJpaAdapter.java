package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseColumn;
import com.corwin.datasource.domain.repo.DatabaseColumnRepository;
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
public class DatabaseColumnRepositoryJpaAdapter implements DatabaseColumnRepository {

    private final DatabaseColumnJpaRepository repo;
    private final DatabaseColumnMybatisMapper mybatisMapper;

    @Override
    public <S extends DatabaseColumn> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends DatabaseColumn> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<DatabaseColumn> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(DatabaseColumn entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<DatabaseColumn> findByTableId(Long tableId) {
        return repo.findByTableId(tableId);
    }

    @Override
    public PageData<DatabaseColumn> findByTableId(Long tableId, PageSpec spec) {
        return mybatisMapper.page(tableId, null, spec);
    }

    @Override
    public PageData<DatabaseColumn> findByTableIdAndColumnNameContainingIgnoreCase(Long tableId, String nameLike,
            PageSpec spec) {
        return mybatisMapper.page(tableId, LikePatternUtils.toContainsPattern(nameLike), spec);
    }

    @Override
    public void deleteByDatabaseId(Long databaseId) {
        repo.deleteByDatabaseId(databaseId);
        repo.flush();
    }

    @Override
    public void deleteByTableId(Long tableId) {
        repo.deleteByTableId(tableId);
        repo.flush();
    }
}

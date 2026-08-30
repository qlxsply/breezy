package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseTable;
import com.corwin.datasource.domain.repo.DatabaseTablePageQuery;
import com.corwin.datasource.domain.repo.DatabaseTableRepository;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.framework.util.StrUtil;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * @author Corwin 2026/1/11
 */
@Repository
@RequiredArgsConstructor
public class DatabaseTableRepositoryJpaAdapter implements DatabaseTableRepository {

  private final DatabaseTableJpaRepository repo;
  private final DatabaseTableMybatisMapper mybatisMapper;

  @Override
  public <S extends DatabaseTable> S save(S entity) {
    return repo.save(entity);
  }

  @Override
  public <S extends DatabaseTable> List<S> saveAll(Iterable<S> entities) {
    return repo.saveAll(entities);
  }

  @Override
  public Optional<DatabaseTable> findById(Long id) {
    return repo.findById(id);
  }

  @Override
  public boolean existsById(Long id) {
    return repo.existsById(id);
  }

  @Override
  public void delete(DatabaseTable entity) {
    repo.delete(entity);
    repo.flush();
  }

  @Override
  public void deleteById(Long id) {
    repo.deleteById(id);
    repo.flush();
  }

  @Override
  public void deleteByDatabaseId(Long databaseId) {
    repo.deleteByDatabaseId(databaseId);
    repo.flush();
  }

  @Override
  public List<DatabaseTable> findByDatabaseId(Long databaseId) {
    return repo.findByDatabaseId(databaseId);
  }

  @Override
  public PageData<DatabaseTable> pageByQuery(DatabaseTablePageQuery query, PageSpec spec) {
    if (query == null || query.databaseId() == null) {
      throw new IllegalArgumentException("databaseId required");
    }
    return mybatisMapper.pageByQuery(normalizeQuery(query), spec);
  }

  private DatabaseTablePageQuery normalizeQuery(DatabaseTablePageQuery query) {
    return new DatabaseTablePageQuery(
        query.databaseId(),
        StrUtil.trimToNull(query.tableSchema()),
        LikePatternUtils.toContainsPattern(query.tableNameLike()),
        StrUtil.trimToNull(query.tableType()));
  }
}

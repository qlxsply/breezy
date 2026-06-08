package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseTable;
import com.corwin.datasource.domain.repo.DatabaseTablePageQuery;
import com.corwin.datasource.domain.repo.DatabaseTableRepository;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XTableQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/1/11
 */
@Repository
@RequiredArgsConstructor
public class DatabaseTableRepositoryJpaAdapter implements DatabaseTableRepository {

    private final DatabaseTableJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

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
    public PageData<DatabaseTable> findByDatabaseId(Long databaseId, PageSpec spec) {
        return JpaPageMapper.toPageData(repo.findByDatabaseId(databaseId, JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<DatabaseTable> findByDatabaseIdAndTableSchema(Long databaseId, String tableSchema, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByDatabaseIdAndTableSchema(databaseId, tableSchema, JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<DatabaseTable> findByDatabaseIdAndTableNameContainingIgnoreCase(Long databaseId, String nameLike,
            PageSpec spec) {
        return JpaPageMapper.toPageData(repo.findByDatabaseIdAndTableNameContainingIgnoreCase(databaseId, nameLike,
                JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<DatabaseTable> findByDatabaseIdAndTableSchemaAndTableNameContainingIgnoreCase(Long databaseId,
            String tableSchema, String nameLike, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByDatabaseIdAndTableSchemaAndTableNameContainingIgnoreCase(databaseId, tableSchema, nameLike,
                        JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<DatabaseTable> findByDatabaseIdAndTableType(Long databaseId, String tableType, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByDatabaseIdAndTableType(databaseId, tableType, JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<DatabaseTable> findByDatabaseIdAndTableSchemaAndTableType(Long databaseId, String tableSchema,
            String tableType, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByDatabaseIdAndTableSchemaAndTableType(databaseId, tableSchema, tableType,
                        JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<DatabaseTable> pageByQuery(DatabaseTablePageQuery query, PageSpec spec) {
        if (query == null || query.databaseId() == null) {
            throw new IllegalArgumentException("databaseId required");
        }

        String schema = StrUtil.trimToNull(query.tableSchema());
        String nameLike = StrUtil.trimToNull(query.tableNameLike());
        String tableType = StrUtil.trimToNull(query.tableType());
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;

        XTableQuery<DatabaseTable, DatabaseTable> dynamicQuery = xSql.using(dataSource)
                .table(DatabaseTable.class, DatabaseTable.class)
                .eq(DatabaseTable::getDatabaseId, query.databaseId())
                .eqIf(StrUtil.isNotBlank(schema), DatabaseTable::getTableSchema, schema)
                .likeIf(StrUtil.isNotBlank(nameLike), DatabaseTable::getTableName, "%" + nameLike + "%")
                .eqIf(StrUtil.isNotBlank(tableType), DatabaseTable::getTableType, tableType);
        applySort(dynamicQuery, resolvedSpec);
        return dynamicQuery.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    private void applySort(XTableQuery<DatabaseTable, DatabaseTable> query, PageSpec spec) {
        if (spec == null || spec.sorts().isEmpty()) {
            return;
        }
        for (SortSpec sort : spec.sorts()) {
            if (sort == null) {
                continue;
            }
            String field = StrUtil.trimToNull(sort.field());
            if (field == null) {
                continue;
            }
            query.orderBy(field, toXSortDirection(sort.direction()));
        }
    }

    private XSortDirection toXSortDirection(SortDirection direction) {
        if (direction == SortDirection.DESC) {
            return XSortDirection.DESC;
        }
        return XSortDirection.ASC;
    }
}

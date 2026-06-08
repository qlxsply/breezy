package com.corwin.datasource.infrastructure.persistence;

import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseType;
import com.corwin.datasource.domain.repo.DatabaseSourcePageQuery;
import com.corwin.datasource.domain.repo.DatabaseSourceRepository;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XTableQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Corwin 2026/1/11
 */
@Repository
@RequiredArgsConstructor
public class DatabaseSourceRepositoryJpaAdapter implements DatabaseSourceRepository {

    private final DatabaseSourceJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

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
        DatabaseType dbType = query == null ? null : query.dbType();
        String nameLike = query == null ? null : StrUtil.trimToNull(query.nameLike());

        XTableQuery<DatabaseSource, DatabaseSource> dynamicQuery = xSql.using(dataSource)
                .table(DatabaseSource.class, DatabaseSource.class)
                .eqIf(dbType != null, DatabaseSource::getDbType, dbType)
                .likeIf(StrUtil.isNotBlank(nameLike), DatabaseSource::getName, "%" + nameLike + "%");
        applySort(dynamicQuery, resolvedSpec);
        return dynamicQuery.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    private void applySort(XTableQuery<DatabaseSource, DatabaseSource> query, PageSpec spec) {
        if (spec == null || spec.sorts().isEmpty()) {
            return;
        }
        for (SortSpec sort : spec.sorts()) {
            if (sort == null) {
                continue;
            }
            String field = normalizeSortField(sort.field());
            if (field == null) {
                continue;
            }
            query.orderBy(field, toXSortDirection(sort.direction()));
        }
    }

    private String normalizeSortField(String field) {
        String normalized = StrUtil.trimToNull(field);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "NAME" -> "name";
            case "DB_TYPE", "DBTYPE" -> "dbType";
            case "STATUS", "DS_STATUS" -> "status";
            case "SOURCE_TYPE", "SOURCETYPE" -> "sourceType";
            case "LAST_TEST_TIME", "LASTTESTTIME" -> "lastTestTime";
            case "LAST_OK_TIME", "LASTOKTIME" -> "lastOkTime";
            default -> normalized;
        };
    }

    private XSortDirection toXSortDirection(SortDirection direction) {
        if (direction == SortDirection.DESC) {
            return XSortDirection.DESC;
        }
        return XSortDirection.ASC;
    }
}

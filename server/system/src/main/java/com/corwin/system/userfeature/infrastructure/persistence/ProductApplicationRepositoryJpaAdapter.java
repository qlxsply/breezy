package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XNativeQuery;
import com.corwin.system.userfeature.domain.model.ProductApplication;
import com.corwin.system.userfeature.domain.repo.ProductApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class ProductApplicationRepositoryJpaAdapter implements ProductApplicationRepository {

    private final ProductApplicationJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

    @Override
    public <S extends ProductApplication> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ProductApplication> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ProductApplication> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ProductApplication entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<ProductApplication> page(String keyword, Boolean enabled, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;
        String normalizedKeyword = StrUtil.trimToNull(keyword);
        XNativeQuery<ProductApplication> query = xSql.using(dataSource)
                .nativeQuery(ProductApplication.class)
                .sql("""
                        select id as id,
                               application_code as applicationCode,
                               application_name as applicationName,
                               description as description,
                               icon as icon,
                               route_path as routePath,
                               component_path as componentPath,
                               enabled as enabled,
                               system_built_in as systemBuiltIn,
                               display_order as displayOrder,
                               created_by as createdBy,
                               created_at as createdAt,
                               updated_by as updatedBy,
                               updated_at as updatedAt
                        from sys_product_application
                        """)
                .eqIf(enabled != null, "enabled", enabled)
                .likeIf(normalizedKeyword != null,
                        "concat(coalesce(application_code,''),' ',coalesce(application_name,''))", normalizedKeyword);
        applySort(query, resolvedSpec);
        return query.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    @Override
    public List<ProductApplication> findAll() {
        return repo.findAll();
    }

    @Override
    public List<ProductApplication> findByIdIn(Iterable<Long> ids) {
        return repo.findByIdIn(ids);
    }

    private void applySort(XNativeQuery<ProductApplication> query, PageSpec spec) {
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
            query.orderByAlias(field, toXSortDirection(sort.direction()));
        }
    }

    private String normalizeSortField(String field) {
        String normalized = StrUtil.trimToNull(field);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "CODE", "APPLICATION_CODE", "APPLICATIONCODE" -> "applicationCode";
            case "NAME", "APPLICATION_NAME", "APPLICATIONNAME" -> "applicationName";
            case "ENABLED" -> "enabled";
            case "DISPLAY_ORDER", "DISPLAYORDER", "SORT_NO", "SORTNO" -> "displayOrder";
            case "CREATED_AT", "CREATEDAT" -> "createdAt";
            case "UPDATED_AT", "UPDATEDAT" -> "updatedAt";
            default -> normalized;
        };
    }

    private XSortDirection toXSortDirection(SortDirection direction) {
        return direction == SortDirection.DESC ? XSortDirection.DESC : XSortDirection.ASC;
    }
}

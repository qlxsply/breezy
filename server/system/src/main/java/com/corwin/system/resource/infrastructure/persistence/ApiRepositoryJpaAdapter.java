package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XTableQuery;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.system.resource.domain.model.Api;
import com.corwin.system.resource.domain.repo.ApiPageQuery;
import com.corwin.system.resource.domain.repo.ApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
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
    public PageData<Api> page(ApiPageQuery query, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;
        String module = query == null ? null : StrUtil.trimToNull(query.module());
        String pathPattern = query == null ? null : StrUtil.trimToNull(query.pathPattern());
        String handlerClass = query == null ? null : StrUtil.trimToNull(query.handlerClass());
        String handlerMethod = query == null ? null : StrUtil.trimToNull(query.handlerMethod());
        String userType = query == null || query.userType() == null ? null : query.userType().name();

        XTableQuery<Api, Api> dynamicQuery = xSql.using(dataSource)
                .table(Api.class, Api.class)
                .eqIf(StrUtil.isNotBlank(module), Api::getModule, module)
                .likeIf(StrUtil.isNotBlank(pathPattern), Api::getPathPattern, "%" + pathPattern + "%")
                .likeIf(StrUtil.isNotBlank(handlerClass), Api::getHandlerClass, "%" + handlerClass + "%")
                .likeIf(StrUtil.isNotBlank(handlerMethod), Api::getHandlerMethod, "%" + handlerMethod + "%")
                .eqIf(query != null && query.permissionDeclared() != null, Api::getPermissionDeclared,
                        query.permissionDeclared())
                .eqIf(query != null && query.accessType() != null, Api::getAccessType, query.accessType())
                .likeIf(StrUtil.isNotBlank(userType), Api::getUserTypes, "%" + userType + "%")
                .eqIf(query != null && query.auditDeclared() != null, Api::getAuditDeclared, query.auditDeclared())
                .eqIf(query != null && query.enabled() != null, Api::getEnabled, query.enabled());
        applySort(dynamicQuery, resolvedSpec);
        return dynamicQuery.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    private void applySort(XTableQuery<Api, Api> query, PageSpec spec) {
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
            case "MODULE" -> "module";
            case "PROTOCOL" -> "protocol";
            case "HTTP_METHOD", "HTTPMETHOD", "METHOD" -> "httpMethod";
            case "PATH_PATTERN", "PATHPATTERN", "PATH" -> "pathPattern";
            case "HANDLER_CLASS", "HANDLERCLASS" -> "handlerClass";
            case "HANDLER_METHOD", "HANDLERMETHOD" -> "handlerMethod";
            case "PERMISSION_DECLARED", "PERMISSIONDECLARED" -> "permissionDeclared";
            case "ACCESS_TYPE", "ACCESSTYPE" -> "accessType";
            case "AUDIT_DECLARED", "AUDITDECLARED" -> "auditDeclared";
            case "ENABLED", "STATUS" -> "enabled";
            case "CREATED_AT", "CREATEDAT" -> "createdAt";
            case "UPDATED_AT", "UPDATEDAT" -> "updatedAt";
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

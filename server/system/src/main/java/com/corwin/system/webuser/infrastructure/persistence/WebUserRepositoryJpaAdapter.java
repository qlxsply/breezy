package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XNativeQuery;
import com.corwin.system.webuser.domain.model.WebUser;
import com.corwin.system.webuser.domain.model.WebUserStatus;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
@Repository
@RequiredArgsConstructor
public class WebUserRepositoryJpaAdapter implements WebUserRepository {

    private final WebUserJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

    @Override
    public <S extends WebUser> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends WebUser> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<WebUser> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(WebUser entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<WebUser> findAll(PageSpec spec) {
        return JpaPageMapper.toPageData(repo.findAll(JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<WebUser> page(String keyword, WebUserStatus status, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;
        String normalizedKeyword = StrUtil.trimToNull(keyword);
        XNativeQuery<WebUser> query = xSql.using(dataSource)
                .nativeQuery(WebUser.class)
                .sql("""
                        select id as id,
                               display_name as displayName,
                               nickname as nickname,
                               status as status,
                               register_method as registerMethod,
                               register_channel as registerChannel,
                               register_source as registerSource,
                               register_ip as registerIp,
                               register_user_agent as registerUserAgent,
                               primary_identity_id as primaryIdentityId,
                               token_version as tokenVersion,
                               token_not_before as tokenNotBefore,
                               last_login_at as lastLoginAt,
                               last_login_ip as lastLoginIp,
                               disabled_at as disabledAt,
                               disabled_by as disabledBy,
                               disabled_reason as disabledReason,
                               cancelled_at as cancelledAt,
                               cancel_reason as cancelReason,
                               created_at as createdAt,
                               created_by as createdBy,
                               updated_at as updatedAt,
                               updated_by as updatedBy
                        from tb_user
                        """)
                .eqIf(status != null, "status", status)
                .likeIf(normalizedKeyword != null,
                        "concat(coalesce(display_name,''),' ',coalesce(nickname,''))", normalizedKeyword);
        applySort(query, resolvedSpec);
        return query.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    @Override
    public PageData<WebUser> findByStatus(WebUserStatus status, PageSpec spec) {
        return JpaPageMapper.toPageData(repo.findByStatus(status, JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<WebUser> findByKeyword(String keyword, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByDisplayNameContainingIgnoreCaseOrNicknameContainingIgnoreCase(keyword, keyword,
                        JpaPageMapper.toPageable(spec)));
    }

    @Override
    public PageData<WebUser> findByStatusAndKeyword(WebUserStatus status, String keyword, PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByStatusAndDisplayNameContainingIgnoreCaseOrStatusAndNicknameContainingIgnoreCase(status,
                        keyword, status, keyword, JpaPageMapper.toPageable(spec)));
    }

    private void applySort(XNativeQuery<WebUser> query, PageSpec spec) {
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
            case "ACCOUNT", "USERNAME", "DISPLAY_NAME", "DISPLAYNAME" -> "displayName";
            case "NICKNAME" -> "nickname";
            case "STATUS" -> "status";
            case "LAST_LOGIN_AT", "LASTLOGINAT" -> "lastLoginAt";
            case "CREATED_AT", "CREATEDAT" -> "createdAt";
            case "UPDATED_AT", "UPDATEDAT" -> "updatedAt";
            default -> normalized;
        };
    }

    private XSortDirection toXSortDirection(SortDirection direction) {
        return direction == SortDirection.DESC ? XSortDirection.DESC : XSortDirection.ASC;
    }
}

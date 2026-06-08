package com.corwin.system.notify.infrastructure.persistence;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XTableQuery;
import com.corwin.system.notify.domain.model.Notification;
import com.corwin.system.notify.domain.repo.NotificationPageQuery;
import com.corwin.system.notify.domain.repo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class NotificationRepositoryJpaAdapter implements NotificationRepository {

    private final NotificationJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

    @Override
    public <S extends Notification> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Notification> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Notification entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<Notification> findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(Long userId,
            UserType userType) {
        return repo.findByUserIdAndUserTypeAndIsReadFalseOrderByCreatedAtDesc(userId, userType);
    }

    @Override
    public Optional<Notification> findByIdAndUserIdAndUserType(Long id, Long userId, UserType userType) {
        return repo.findByIdAndUserIdAndUserType(id, userId, userType);
    }

    @Override
    public List<Notification> findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(Long userId,
            UserType userType, Instant after) {
        return repo.findByUserIdAndUserTypeAndIsReadFalseAndCreatedAtAfterOrderByCreatedAtDesc(userId, userType,
                after);
    }

    @Override
    public PageData<Notification> pageByQuery(NotificationPageQuery query, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;

        XTableQuery<Notification, Notification> dynamicQuery = xSql.using(dataSource)
                .table(Notification.class, Notification.class)
                .eq(Notification::getUserId, query.userId())
                .eq(Notification::getUserType, query.userType())
                .eqIf(query.unreadOnly(), "isRead", false);
        applySort(dynamicQuery, resolvedSpec);
        return dynamicQuery.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    @Override
    public long countByUserIdAndUserTypeAndIsReadFalse(Long userId, UserType userType) {
        return repo.countByUserIdAndUserTypeAndIsReadFalse(userId, userType);
    }

    @Override
    public void markAllReadByUserIdAndUserType(Long userId, UserType userType) {
        repo.markAllReadByUserIdAndUserType(userId, userType, HighDate.mockInstant());
    }

    private void applySort(XTableQuery<Notification, Notification> query, PageSpec spec) {
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
            case "USER_ID", "USERID" -> "userId";
            case "USER_TYPE", "USERTYPE" -> "userType";
            case "TITLE" -> "title";
            case "CONTENT" -> "content";
            case "MSG_TYPE", "MSGTYPE" -> "msgType";
            case "PRIORITY" -> "priority";
            case "ROUTE" -> "route";
            case "READ", "IS_READ", "ISREAD" -> "isRead";
            case "CREATED_AT", "CREATEDAT" -> "createdAt";
            case "READ_AT", "READAT" -> "readAt";
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

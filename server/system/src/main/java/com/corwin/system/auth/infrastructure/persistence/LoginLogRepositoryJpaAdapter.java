package com.corwin.system.auth.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XTableQuery;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.auth.domain.model.LoginEventType;
import com.corwin.system.auth.domain.repo.LoginLogPageQuery;
import com.corwin.system.auth.domain.repo.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Corwin 2026/4/15
 */
@Repository
@RequiredArgsConstructor
public class LoginLogRepositoryJpaAdapter implements LoginLogRepository {

    private final LoginLogJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

    @Override
    public <S extends LoginEvent> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends LoginEvent> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<LoginEvent> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(LoginEvent entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<LoginEvent> pageByQuery(LoginLogPageQuery query, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;
        String userAccount = query == null ? null : StrUtil.trimToNull(query.userAccount());
        List<LoginEventType> eventTypes = query == null || query.eventTypes() == null ? List.of()
                : query.eventTypes().stream().filter(it -> it != null).toList();

        XTableQuery<LoginEvent, LoginEvent> dynamicQuery = xSql.using(dataSource)
                .table(LoginEvent.class, LoginEvent.class)
                .eqIf(userAccount != null, LoginEvent::getUsername, userAccount)
                .inIf(!eventTypes.isEmpty(), LoginEvent::getEventType, eventTypes)
                .geIf(query != null && query.startAt() != null && query.startInclusive(), LoginEvent::getOccurredAt,
                        query.startAt())
                .gtIf(query != null && query.startAt() != null && !query.startInclusive(), LoginEvent::getOccurredAt,
                        query.startAt())
                .ltIf(query != null && query.endAt() != null, LoginEvent::getOccurredAt, query.endAt());
        applySort(dynamicQuery, resolvedSpec);
        return dynamicQuery.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    private void applySort(XTableQuery<LoginEvent, LoginEvent> query, PageSpec spec) {
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
            case "USER_ACCOUNT", "USERACCOUNT", "USERNAME" -> "username";
            case "EVENT", "EVENTTYPE" -> "eventType";
            case "SUCCESS" -> "success";
            case "CLIENT_IP", "CLIENTIP", "LOGIN_IP", "LOGINIP" -> "loginIp";
            case "MESSAGE", "FAILURE_REASON", "FAILUREREASON" -> "failureReason";
            case "CREATED_AT", "CREATEDAT", "OCCURRED_AT", "OCCURREDAT" -> "occurredAt";
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

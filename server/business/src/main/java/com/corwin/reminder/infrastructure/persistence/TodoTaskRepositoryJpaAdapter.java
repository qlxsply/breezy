package com.corwin.reminder.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.persistence.jpa.JpaPageMapper;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.xsql.XSortDirection;
import com.corwin.framework.xsql.XSql;
import com.corwin.framework.xsql.XTableQuery;
import com.corwin.reminder.domain.model.TodoTask;
import com.corwin.reminder.domain.model.TodoTaskStatus;
import com.corwin.reminder.domain.repo.TodoTaskPageQuery;
import com.corwin.reminder.domain.repo.TodoTaskRepository;
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
public class TodoTaskRepositoryJpaAdapter implements TodoTaskRepository {

    private final TodoTaskJpaRepository repo;
    private final XSql xSql;
    private final DataSource dataSource;

    @Override
    public <S extends TodoTask> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends TodoTask> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<TodoTask> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(TodoTask entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<TodoTask> pageByQuery(TodoTaskPageQuery query, PageSpec spec) {
        PageSpec resolvedSpec = spec == null ? PageSpec.of(null, null, List.of()) : spec;
        List<TodoTaskStatus> statuses = query == null ? null : query.statuses();
        String contentLike = query == null ? null : StrUtil.trimToNull(query.contentLike());

        XTableQuery<TodoTask, TodoTask> dynamicQuery = xSql.using(dataSource)
                .table(TodoTask.class, TodoTask.class)
                .inIf(statuses != null && !statuses.isEmpty(), TodoTask::getStatus, statuses)
                .likeIf(StrUtil.isNotBlank(contentLike), TodoTask::getContent, "%" + contentLike + "%");
        applySort(dynamicQuery, resolvedSpec);
        return dynamicQuery.page(resolvedSpec.pageNo(), resolvedSpec.pageSize());
    }

    @Override
    public PageData<TodoTask> findByStatusInAndRemindAtLessThanEqual(List<TodoTaskStatus> statuses, Instant now,
            PageSpec spec) {
        return JpaPageMapper.toPageData(
                repo.findByStatusInAndRemindAtLessThanEqual(statuses, now, JpaPageMapper.toPageable(spec)));
    }

    @Override
    public List<TodoTask> findByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repo.findByIdIn(ids);
    }

    @Override
    public List<TodoTask> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant startInclusive,
            Instant endExclusive) {
        return repo.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startInclusive, endExclusive);
    }

    @Override
    public List<TodoTask> findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(Instant startInclusive,
            Instant endExclusive) {
        return repo.findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(startInclusive, endExclusive);
    }

    private void applySort(XTableQuery<TodoTask, TodoTask> query, PageSpec spec) {
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
            case "CONTENT" -> "content";
            case "DUE_TIME", "DUETIME" -> "dueTime";
            case "REMIND_AT", "REMINDAT" -> "remindAt";
            case "STATUS", "TASK_STATUS" -> "status";
            case "SORT_NO", "SORTNO" -> "sortNo";
            case "OWNER_ID", "OWNERID" -> "ownerId";
            case "COMPLETED_AT", "COMPLETEDAT" -> "completedAt";
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

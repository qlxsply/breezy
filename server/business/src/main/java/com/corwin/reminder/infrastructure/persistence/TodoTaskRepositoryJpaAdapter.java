package com.corwin.reminder.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.reminder.domain.model.TodoTask;
import com.corwin.reminder.domain.model.TodoTaskStatus;
import com.corwin.reminder.domain.repo.TodoTaskPageQuery;
import com.corwin.reminder.domain.repo.TodoTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class TodoTaskRepositoryJpaAdapter implements TodoTaskRepository {

    private final TodoTaskJpaRepository repo;
    private final TodoTaskMybatisMapper mybatisMapper;

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
        return mybatisMapper.pageByQuery(normalizeQuery(query), spec);
    }

    @Override
    public PageData<TodoTask> findByStatusInAndRemindAtLessThanEqual(List<TodoTaskStatus> statuses, Instant now,
            PageSpec spec) {
        return mybatisMapper.findByStatusInAndRemindAtLessThanEqual(statuses, now, spec);
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

    private TodoTaskPageQuery normalizeQuery(TodoTaskPageQuery query) {
        if (query == null) {
            return null;
        }
        List<TodoTaskStatus> statuses = query.statuses() == null ? List.of() : query.statuses().stream()
                .filter(Objects::nonNull).toList();
        return new TodoTaskPageQuery(statuses, LikePatternUtils.toContainsPattern(query.contentLike()));
    }
}

package com.corwin.reminder.domain.repo;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.framework.domain.repo.DynamicPageQueryRepository;
import com.corwin.reminder.domain.model.TodoTask;
import com.corwin.reminder.domain.model.TodoTaskStatus;
import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/1/12
 */
public interface TodoTaskRepository
    extends DomainRepository<TodoTask, Long>,
        DynamicPageQueryRepository<TodoTask, TodoTaskPageQuery> {

  PageData<TodoTask> findByStatusInAndRemindAtLessThanEqual(
      List<TodoTaskStatus> statuses, Instant now, PageSpec spec);

  List<TodoTask> findByIdIn(List<Long> ids);

  List<TodoTask> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Instant startInclusive, Instant endExclusive);

  List<TodoTask> findByCompletedAtGreaterThanEqualAndCompletedAtLessThan(
      Instant startInclusive, Instant endExclusive);
}

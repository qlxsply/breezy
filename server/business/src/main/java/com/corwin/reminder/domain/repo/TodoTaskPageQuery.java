package com.corwin.reminder.domain.repo;

import com.corwin.reminder.domain.model.TodoTaskStatus;

import java.util.List;

/**
 * 待办分页查询条件。
 *
 * @author Corwin 2026/4/15
 */
public record TodoTaskPageQuery(
        List<TodoTaskStatus> statuses,
        String contentLike
) {
}

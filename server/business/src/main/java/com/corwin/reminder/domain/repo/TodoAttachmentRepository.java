package com.corwin.reminder.domain.repo;

import com.corwin.reminder.domain.model.TodoAttachment;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public interface TodoAttachmentRepository extends DomainRepository<TodoAttachment, Long> {

    List<TodoAttachment> findByTodoIdOrderBySortNoAsc(Long todoId);

    List<TodoAttachment> findByTodoIdInOrderByTodoIdAscSortNoAsc(List<Long> todoIds);

    void deleteByTodoId(Long todoId);
}

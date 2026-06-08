package com.corwin.reminder.infrastructure.persistence;

import com.corwin.reminder.domain.model.TodoAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Corwin 2026/3/12
 */
public interface TodoAttachmentJpaRepository extends JpaRepository<TodoAttachment, Long> {

    List<TodoAttachment> findByTodoIdOrderBySortNoAsc(Long todoId);

    List<TodoAttachment> findByTodoIdInOrderByTodoIdAscSortNoAsc(List<Long> todoIds);

    void deleteByTodoId(Long todoId);
}

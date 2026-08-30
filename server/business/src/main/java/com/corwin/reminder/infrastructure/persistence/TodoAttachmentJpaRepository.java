package com.corwin.reminder.infrastructure.persistence;

import com.corwin.reminder.domain.model.TodoAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/3/12
 */
public interface TodoAttachmentJpaRepository extends JpaRepository<TodoAttachment, Long> {

  List<TodoAttachment> findByTodoIdOrderBySortNoAsc(Long todoId);

  List<TodoAttachment> findByTodoIdInOrderByTodoIdAscSortNoAsc(List<Long> todoIds);

  void deleteByTodoId(Long todoId);
}

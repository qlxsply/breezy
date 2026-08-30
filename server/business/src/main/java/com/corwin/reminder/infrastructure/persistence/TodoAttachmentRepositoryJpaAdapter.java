package com.corwin.reminder.infrastructure.persistence;

import com.corwin.reminder.domain.model.TodoAttachment;
import com.corwin.reminder.domain.repo.TodoAttachmentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * @author Corwin 2026/3/12
 */
@Repository
@RequiredArgsConstructor
public class TodoAttachmentRepositoryJpaAdapter implements TodoAttachmentRepository {

  private final TodoAttachmentJpaRepository repo;

  @Override
  public <S extends TodoAttachment> S save(S entity) {
    return repo.save(entity);
  }

  @Override
  public <S extends TodoAttachment> List<S> saveAll(Iterable<S> entities) {
    return repo.saveAll(entities);
  }

  @Override
  public Optional<TodoAttachment> findById(Long id) {
    return repo.findById(id);
  }

  @Override
  public boolean existsById(Long id) {
    return repo.existsById(id);
  }

  @Override
  public void delete(TodoAttachment entity) {
    repo.delete(entity);
    repo.flush();
  }

  @Override
  public void deleteById(Long id) {
    repo.deleteById(id);
    repo.flush();
  }

  @Override
  public List<TodoAttachment> findByTodoIdOrderBySortNoAsc(Long todoId) {
    return repo.findByTodoIdOrderBySortNoAsc(todoId);
  }

  @Override
  public List<TodoAttachment> findByTodoIdInOrderByTodoIdAscSortNoAsc(List<Long> todoIds) {
    if (todoIds == null || todoIds.isEmpty()) {
      return List.of();
    }
    return repo.findByTodoIdInOrderByTodoIdAscSortNoAsc(todoIds);
  }

  @Override
  public void deleteByTodoId(Long todoId) {
    repo.deleteByTodoId(todoId);
    repo.flush();
  }
}

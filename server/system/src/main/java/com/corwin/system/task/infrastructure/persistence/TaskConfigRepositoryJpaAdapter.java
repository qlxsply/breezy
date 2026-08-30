package com.corwin.system.task.infrastructure.persistence;

import com.corwin.system.task.domain.model.TaskConfig;
import com.corwin.system.task.domain.model.TaskStatus;
import com.corwin.system.task.domain.repo.TaskConfigRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA adapter implementation of {@link TaskConfigRepository}. Delegates all operations to the
 * underlying {@link TaskConfigJpaRepository}.
 *
 * @author Corwin 2026/3/30
 */
@Component
@RequiredArgsConstructor
public class TaskConfigRepositoryJpaAdapter implements TaskConfigRepository {

  private final TaskConfigJpaRepository jpaRepository;

  /** {@inheritDoc} */
  @Override
  public <S extends TaskConfig> S save(S entity) {
    return jpaRepository.save(entity);
  }

  /** {@inheritDoc} */
  @Override
  public <S extends TaskConfig> List<S> saveAll(Iterable<S> entities) {
    return jpaRepository.saveAll(entities);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<TaskConfig> findById(String id) {
    return jpaRepository.findById(id);
  }

  /** {@inheritDoc} */
  @Override
  public boolean existsById(String id) {
    return jpaRepository.existsById(id);
  }

  /** {@inheritDoc} */
  @Override
  public void delete(TaskConfig entity) {
    jpaRepository.delete(entity);
    jpaRepository.flush();
  }

  /** {@inheritDoc} */
  @Override
  public void deleteById(String id) {
    jpaRepository.deleteById(id);
    jpaRepository.flush();
  }

  /** {@inheritDoc} */
  @Override
  public List<TaskConfig> findAllByTaskStatus(TaskStatus taskStatus) {
    return jpaRepository.findAllByTaskStatus(taskStatus);
  }
}

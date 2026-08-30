package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.system.scheduler.domain.model.SchedulerJobRuntime;
import com.corwin.system.scheduler.domain.model.SchedulerJobStatus;
import com.corwin.system.scheduler.domain.repo.SchedulerJobRuntimeRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JPA-based adapter implementation of {@link SchedulerJobRuntimeRepository}.
 *
 * @author Corwin 2026/4/15
 */
@Component
@RequiredArgsConstructor
public class SchedulerJobRuntimeRepositoryJpaAdapter implements SchedulerJobRuntimeRepository {

  private final SchedulerJobRuntimeJpaRepository jpaRepository;

  @Override
  public <S extends SchedulerJobRuntime> S save(S entity) {
    return jpaRepository.save(entity);
  }

  @Override
  public <S extends SchedulerJobRuntime> List<S> saveAll(Iterable<S> entities) {
    return jpaRepository.saveAll(entities);
  }

  @Override
  public Optional<SchedulerJobRuntime> findById(String id) {
    return jpaRepository.findById(id);
  }

  @Override
  public boolean existsById(String id) {
    return jpaRepository.existsById(id);
  }

  @Override
  public void delete(SchedulerJobRuntime entity) {
    jpaRepository.delete(entity);
    jpaRepository.flush();
  }

  @Override
  public void deleteById(String id) {
    jpaRepository.deleteById(id);
    jpaRepository.flush();
  }

  /** {@inheritDoc} */
  @Override
  public List<SchedulerJobRuntime> findAllByStatus(SchedulerJobStatus status) {
    return jpaRepository.findAllByStatus(status);
  }
}

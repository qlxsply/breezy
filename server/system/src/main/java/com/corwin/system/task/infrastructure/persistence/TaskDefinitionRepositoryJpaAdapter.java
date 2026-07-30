package com.corwin.system.task.infrastructure.persistence;

import com.corwin.system.task.domain.model.TaskDefinition;
import com.corwin.system.task.domain.repo.TaskDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link TaskDefinitionRepository}.
 * Delegates all operations to the underlying {@link TaskDefinitionJpaRepository}.
 *
 * @author Corwin 2026/3/30
 */
@Component
@RequiredArgsConstructor
public class TaskDefinitionRepositoryJpaAdapter implements TaskDefinitionRepository {

    private final TaskDefinitionJpaRepository jpaRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends TaskDefinition> S save(S entity) {
        return jpaRepository.save(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends TaskDefinition> List<S> saveAll(Iterable<S> entities) {
        return jpaRepository.saveAll(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<TaskDefinition> findById(String id) {
        return jpaRepository.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(TaskDefinition entity) {
        jpaRepository.delete(entity);
        jpaRepository.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
        jpaRepository.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TaskDefinition> findAllByRemovedFalse() {
        return jpaRepository.findAllByRemovedFalse();
    }
}

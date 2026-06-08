package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.system.scheduler.domain.model.SchedulerJobDefinition;
import com.corwin.system.scheduler.domain.model.SchedulerJobSource;
import com.corwin.system.scheduler.domain.repo.SchedulerJobDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/4/15
 */
@Component
@RequiredArgsConstructor
public class SchedulerJobDefinitionRepositoryJpaAdapter implements SchedulerJobDefinitionRepository {

    private final SchedulerJobDefinitionJpaRepository jpaRepository;

    @Override
    public <S extends SchedulerJobDefinition> S save(S entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public <S extends SchedulerJobDefinition> List<S> saveAll(Iterable<S> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<SchedulerJobDefinition> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void delete(SchedulerJobDefinition entity) {
        jpaRepository.delete(entity);
        jpaRepository.flush();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
        jpaRepository.flush();
    }

    @Override
    public List<SchedulerJobDefinition> findAllByDeletedFalse() {
        return jpaRepository.findAllByDeletedFalse();
    }

    @Override
    public List<SchedulerJobDefinition> findAllByEnabledTrueAndDeletedFalse() {
        return jpaRepository.findAllByEnabledTrueAndDeletedFalse();
    }

    @Override
    public List<SchedulerJobDefinition> findAllBySourceAndDeletedFalse(SchedulerJobSource source) {
        return jpaRepository.findAllBySourceAndDeletedFalse(source);
    }
}

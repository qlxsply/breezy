package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;
import com.corwin.system.scheduler.domain.repo.SchedulerJobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * JPA-based adapter implementation of {@link SchedulerJobExecutionRepository}.
 *
 * @author Corwin 2026/4/15
 */
@Component
@RequiredArgsConstructor
public class SchedulerJobExecutionRepositoryJpaAdapter implements SchedulerJobExecutionRepository {

    private final SchedulerJobExecutionJpaRepository jpaRepository;
    private final SchedulerJobExecutionMybatisMapper mybatisMapper;

    @Override
    public <S extends SchedulerJobExecution> S save(S entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public <S extends SchedulerJobExecution> List<S> saveAll(Iterable<S> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<SchedulerJobExecution> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void delete(SchedulerJobExecution entity) {
        jpaRepository.delete(entity);
        jpaRepository.flush();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
        jpaRepository.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageData<SchedulerJobExecution> pageByJobId(String jobId, PageSpec spec) {
        return mybatisMapper.pageByJobId(jobId, spec);
    }
}

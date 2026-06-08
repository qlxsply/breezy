package com.corwin.system.scheduler.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.system.scheduler.domain.model.SchedulerJobExecution;
import com.corwin.system.scheduler.domain.repo.SchedulerJobExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/4/15
 */
@Component
@RequiredArgsConstructor
public class SchedulerJobExecutionRepositoryJpaAdapter implements SchedulerJobExecutionRepository {

    private final SchedulerJobExecutionJpaRepository jpaRepository;

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

    @Override
    public PageData<SchedulerJobExecution> pageByJobId(String jobId, PageSpec spec) {
        PageSpec finalSpec = spec == null ? PageSpec.of(1, 20, List.of()) : spec;
        var page = jpaRepository.findByJobIdOrderByStartTimeDesc(jobId,
                PageRequest.of(Math.max(0, finalSpec.pageNo() - 1), finalSpec.pageSize()));
        return PageData.of(finalSpec.pageNo(), finalSpec.pageSize(), page.getTotalElements(), page.getContent());
    }
}

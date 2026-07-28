package com.corwin.reminder.infrastructure.persistence;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.mybatis.LikePatternUtils;
import com.corwin.reminder.domain.model.ScheduleEvent;
import com.corwin.reminder.domain.model.ScheduleEventStatus;
import com.corwin.reminder.domain.repo.ScheduleEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class ScheduleEventRepositoryJpaAdapter implements ScheduleEventRepository {

    private final ScheduleEventJpaRepository repo;
    private final ScheduleEventMybatisMapper mybatisMapper;

    @Override
    public <S extends ScheduleEvent> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ScheduleEvent> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ScheduleEvent> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ScheduleEvent entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public PageData<ScheduleEvent> findAll(PageSpec spec) {
        return mybatisMapper.page(null, null, spec == null ? PageSpec.of(null, null, List.of()) : spec);
    }

    @Override
    public PageData<ScheduleEvent> findByStatus(ScheduleEventStatus status, PageSpec spec) {
        return mybatisMapper.page(status, null, spec == null ? PageSpec.of(null, null, List.of()) : spec);
    }

    @Override
    public PageData<ScheduleEvent> findByStatusAndTitleContainingIgnoreCase(ScheduleEventStatus status,
            String titleLike, PageSpec spec) {
        return mybatisMapper.page(status, LikePatternUtils.toContainsPattern(titleLike),
                spec == null ? PageSpec.of(null, null, List.of()) : spec);
    }
}

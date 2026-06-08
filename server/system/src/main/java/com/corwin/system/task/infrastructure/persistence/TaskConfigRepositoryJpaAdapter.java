package com.corwin.system.task.infrastructure.persistence;

import com.corwin.system.task.domain.model.TaskConfig;
import com.corwin.system.task.domain.model.TaskStatus;
import com.corwin.system.task.domain.repo.TaskConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 任务配置仓储 JPA 适配器
 *
 * @author Corwin 2026/3/30
 */
@Component
@RequiredArgsConstructor
public class TaskConfigRepositoryJpaAdapter implements TaskConfigRepository {

    private final TaskConfigJpaRepository jpaRepository;

    @Override
    public <S extends TaskConfig> S save(S entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public <S extends TaskConfig> List<S> saveAll(Iterable<S> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<TaskConfig> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void delete(TaskConfig entity) {
        jpaRepository.delete(entity);
        jpaRepository.flush();
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
        jpaRepository.flush();
    }

    @Override
    public List<TaskConfig> findAllByTaskStatus(TaskStatus taskStatus) {
        return jpaRepository.findAllByTaskStatus(taskStatus);
    }
}

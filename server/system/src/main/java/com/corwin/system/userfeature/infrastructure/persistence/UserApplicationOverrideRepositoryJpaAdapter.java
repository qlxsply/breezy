package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserApplicationOverride;
import com.corwin.system.userfeature.domain.repo.UserApplicationOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class UserApplicationOverrideRepositoryJpaAdapter implements UserApplicationOverrideRepository {

    private final UserApplicationOverrideJpaRepository repo;

    @Override
    public <S extends UserApplicationOverride> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends UserApplicationOverride> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<UserApplicationOverride> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(UserApplicationOverride entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<UserApplicationOverride> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        repo.deleteByUserId(userId);
        repo.flush();
    }
}

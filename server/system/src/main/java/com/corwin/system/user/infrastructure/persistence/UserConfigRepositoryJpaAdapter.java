package com.corwin.system.user.infrastructure.persistence;

import com.corwin.system.user.domain.model.UserConfig;
import com.corwin.system.user.domain.repo.UserConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-based adapter implementation of {@link UserConfigRepository}.
 * Delegates all operations to {@link UserConfigJpaRepository}.
 *
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class UserConfigRepositoryJpaAdapter implements UserConfigRepository {

    private final UserConfigJpaRepository repo;

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends UserConfig> S save(S entity) {
        return repo.save(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends UserConfig> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserConfig> findById(Long id) {
        return repo.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserConfig> findAll() {
        return repo.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(UserConfig entity) {
        repo.delete(entity);
        repo.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserConfig> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserConfig> findByUserIdAndConfigCode(Long userId, String configCode) {
        return repo.findByUserIdAndConfigCode(userId, configCode);
    }

}

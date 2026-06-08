package com.corwin.system.user.infrastructure.persistence;

import com.corwin.system.user.domain.model.UserConfig;
import com.corwin.system.user.domain.repo.UserConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class UserConfigRepositoryJpaAdapter implements UserConfigRepository {

    private final UserConfigJpaRepository repo;

    @Override
    public <S extends UserConfig> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends UserConfig> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<UserConfig> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public List<UserConfig> findAll() {
        return repo.findAll();
    }

    @Override
    public void delete(UserConfig entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<UserConfig> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public Optional<UserConfig> findByUserIdAndConfigCode(Long userId, String configCode) {
        return repo.findByUserIdAndConfigCode(userId, configCode);
    }

    @Override
    public List<UserConfig> findByConfigCode(String configCode) {
        return repo.findByConfigCode(configCode);
    }

    @Override
    public List<UserConfig> findByConfigCodeAndConfigValue(String configCode, String configValue) {
        return repo.findByConfigCodeAndConfigValue(configCode, configValue);
    }
}

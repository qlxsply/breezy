package com.corwin.system.user.infrastructure.persistence;

import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
@Repository
@RequiredArgsConstructor
public class UserRoleRepositoryJpaAdapter implements UserRoleRepository {

    private final UserRoleJpaRepository repo;

    @Override
    public <S extends UserRole> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends UserRole> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<UserRole> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(UserRole entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<UserRole> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public List<UserRole> findByRoleId(Long roleId) {
        return repo.findByRoleId(roleId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        repo.deleteByUserId(userId);
        repo.flush();
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        repo.deleteByRoleId(roleId);
        repo.flush();
    }
}

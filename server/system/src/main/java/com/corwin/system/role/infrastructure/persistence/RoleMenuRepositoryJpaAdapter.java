package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.RoleMenu;
import com.corwin.system.role.domain.repo.RoleMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/19
 */
@Repository
@RequiredArgsConstructor
public class RoleMenuRepositoryJpaAdapter implements RoleMenuRepository {

    private final RoleMenuJpaRepository repo;

    @Override
    public <S extends RoleMenu> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends RoleMenu> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<RoleMenu> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(RoleMenu entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<RoleMenu> findByRoleId(Long roleId) {
        return repo.findByRoleId(roleId);
    }

    @Override
    public List<RoleMenu> findByRoleIdIn(List<Long> roleIds) {
        return repo.findByRoleIdIn(roleIds);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        repo.deleteByRoleId(roleId);
        repo.flush();
    }
}

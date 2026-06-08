package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.RoleFunction;
import com.corwin.system.role.domain.repo.RoleFunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/7
 */
@Repository
@RequiredArgsConstructor
public class RoleFunctionRepositoryJpaAdapter implements RoleFunctionRepository {

    private final RoleFunctionJpaRepository repo;

    @Override
    public <S extends RoleFunction> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends RoleFunction> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<RoleFunction> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(RoleFunction entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<RoleFunction> findByRoleId(Long roleId) {
        return repo.findByRoleId(roleId);
    }

    @Override
    public List<RoleFunction> findByRoleIdIn(List<Long> roleIds) {
        return repo.findByRoleIdIn(roleIds);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        repo.deleteByRoleId(roleId);
        repo.flush();
    }
}

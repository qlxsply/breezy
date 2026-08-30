package com.corwin.system.role.infrastructure.persistence;

import com.corwin.system.role.domain.model.RoleResource;
import com.corwin.system.role.domain.repo.RoleResourceRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * JPA-based adapter implementation of {@link RoleResourceRepository}.
 *
 * @author Corwin 2026/6/29
 */
@Repository
@RequiredArgsConstructor
public class RoleResourceRepositoryJpaAdapter implements RoleResourceRepository {

  private final RoleResourceJpaRepository repo;

  @Override
  public <S extends RoleResource> S save(S entity) {
    return repo.save(entity);
  }

  @Override
  public <S extends RoleResource> List<S> saveAll(Iterable<S> entities) {
    return repo.saveAll(entities);
  }

  @Override
  public Optional<RoleResource> findById(Long id) {
    return repo.findById(id);
  }

  @Override
  public boolean existsById(Long id) {
    return repo.existsById(id);
  }

  @Override
  public void delete(RoleResource entity) {
    repo.delete(entity);
    repo.flush();
  }

  @Override
  public void deleteById(Long id) {
    repo.deleteById(id);
    repo.flush();
  }

  /** {@inheritDoc} */
  @Override
  public List<RoleResource> findByRoleId(Long roleId) {
    return repo.findByRoleId(roleId);
  }

  /** {@inheritDoc} */
  @Override
  public List<RoleResource> findByRoleIdIn(List<Long> roleIds) {
    return repo.findByRoleIdIn(roleIds);
  }

  /** {@inheritDoc} */
  @Override
  public List<RoleResource> findByResourceIdIn(Collection<Long> resourceIds) {
    return repo.findByResourceIdIn(resourceIds);
  }

  /** {@inheritDoc} */
  @Override
  public void deleteByRoleId(Long roleId) {
    repo.deleteByRoleId(roleId);
    repo.flush();
  }

  /** {@inheritDoc} */
  @Override
  public void deleteByResourceIdIn(Collection<Long> resourceIds) {
    repo.deleteByResourceIdIn(resourceIds);
    repo.flush();
  }
}

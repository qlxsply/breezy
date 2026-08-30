package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserPackageApplicationAccess;
import com.corwin.system.userfeature.domain.repo.UserPackageApplicationAccessRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter implementation of {@link UserPackageApplicationAccessRepository}.
 *
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class UserPackageApplicationAccessRepositoryJpaAdapter
    implements UserPackageApplicationAccessRepository {

  private final UserPackageApplicationAccessJpaRepository repo;

  @Override
  public <S extends UserPackageApplicationAccess> S save(S entity) {
    return repo.save(entity);
  }

  @Override
  public <S extends UserPackageApplicationAccess> List<S> saveAll(Iterable<S> entities) {
    return repo.saveAll(entities);
  }

  @Override
  public Optional<UserPackageApplicationAccess> findById(Long id) {
    return repo.findById(id);
  }

  @Override
  public boolean existsById(Long id) {
    return repo.existsById(id);
  }

  @Override
  public void delete(UserPackageApplicationAccess entity) {
    repo.delete(entity);
    repo.flush();
  }

  @Override
  public void deleteById(Long id) {
    repo.deleteById(id);
    repo.flush();
  }

  @Override
  public List<UserPackageApplicationAccess> findByPackageId(Long packageId) {
    return repo.findByPackageId(packageId);
  }

  @Override
  public List<UserPackageApplicationAccess> findByPackageIdIn(Iterable<Long> packageIds) {
    return repo.findByPackageIdIn(packageIds);
  }

  @Override
  public void deleteByPackageId(Long packageId) {
    repo.deleteByPackageId(packageId);
    repo.flush();
  }
}

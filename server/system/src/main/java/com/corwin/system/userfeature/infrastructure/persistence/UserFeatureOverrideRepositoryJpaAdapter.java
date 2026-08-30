package com.corwin.system.userfeature.infrastructure.persistence;

import com.corwin.system.userfeature.domain.model.UserFeatureOverride;
import com.corwin.system.userfeature.domain.repo.UserFeatureOverrideRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter implementation of {@link UserFeatureOverrideRepository}.
 *
 * @author Corwin 2026/6/14
 */
@Repository
@RequiredArgsConstructor
public class UserFeatureOverrideRepositoryJpaAdapter implements UserFeatureOverrideRepository {

  private final UserFeatureOverrideJpaRepository repo;

  @Override
  public <S extends UserFeatureOverride> S save(S entity) {
    return repo.save(entity);
  }

  @Override
  public <S extends UserFeatureOverride> List<S> saveAll(Iterable<S> entities) {
    return repo.saveAll(entities);
  }

  @Override
  public Optional<UserFeatureOverride> findById(Long id) {
    return repo.findById(id);
  }

  @Override
  public boolean existsById(Long id) {
    return repo.existsById(id);
  }

  @Override
  public void delete(UserFeatureOverride entity) {
    repo.delete(entity);
    repo.flush();
  }

  @Override
  public void deleteById(Long id) {
    repo.deleteById(id);
    repo.flush();
  }

  @Override
  public List<UserFeatureOverride> findByUserId(Long userId) {
    return repo.findByUserId(userId);
  }

  @Override
  public void deleteByUserId(Long userId) {
    repo.deleteByUserId(userId);
    repo.flush();
  }
}

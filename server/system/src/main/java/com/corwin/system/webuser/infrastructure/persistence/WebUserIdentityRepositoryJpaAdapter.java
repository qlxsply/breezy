package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import com.corwin.system.webuser.domain.repo.WebUserIdentityRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter implementation of {@link WebUserIdentityRepository}. Delegates CRUD and query
 * operations to the underlying Spring Data JPA repository.
 *
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserIdentityRepositoryJpaAdapter implements WebUserIdentityRepository {

  private final WebUserIdentityJpaRepository repo;

  public WebUserIdentityRepositoryJpaAdapter(WebUserIdentityJpaRepository repo) {
    this.repo = repo;
  }

  /** {@inheritDoc} */
  @Override
  public <S extends WebUserIdentity> S save(S entity) {
    return repo.save(entity);
  }

  /** {@inheritDoc} */
  @Override
  public <S extends WebUserIdentity> List<S> saveAll(Iterable<S> entities) {
    return repo.saveAll(entities);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<WebUserIdentity> findById(Long id) {
    return repo.findById(id);
  }

  /** {@inheritDoc} */
  @Override
  public boolean existsById(Long id) {
    return repo.existsById(id);
  }

  /** {@inheritDoc} */
  @Override
  public void delete(WebUserIdentity entity) {
    repo.delete(entity);
    repo.flush();
  }

  /** {@inheritDoc} */
  @Override
  public void deleteById(Long id) {
    repo.deleteById(id);
    repo.flush();
  }

  /** {@inheritDoc} */
  @Override
  public List<WebUserIdentity> findByUserId(Long userId) {
    return repo.findByUserId(userId);
  }

  /** {@inheritDoc} */
  @Override
  public Optional<WebUserIdentity> findFirstByUserIdAndIdentityType(
      Long userId, WebUserIdentityType identityType) {
    return repo.findFirstByUserIdAndIdentityType(userId, identityType);
  }
}

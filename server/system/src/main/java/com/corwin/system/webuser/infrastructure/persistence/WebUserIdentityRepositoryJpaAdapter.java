package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import com.corwin.system.webuser.domain.repo.WebUserIdentityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserIdentityRepositoryJpaAdapter implements WebUserIdentityRepository {

    private final WebUserIdentityJpaRepository repo;

    public WebUserIdentityRepositoryJpaAdapter(WebUserIdentityJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public <S extends WebUserIdentity> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends WebUserIdentity> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<WebUserIdentity> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(WebUserIdentity entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<WebUserIdentity> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public Optional<WebUserIdentity> findFirstByUserIdAndIdentityType(Long userId, WebUserIdentityType identityType) {
        return repo.findFirstByUserIdAndIdentityType(userId, identityType);
    }
}

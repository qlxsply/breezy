package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserCurrentIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import com.corwin.system.webuser.domain.repo.WebUserCurrentIdentityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link WebUserCurrentIdentityRepository}.
 *
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserCurrentIdentityRepositoryJpaAdapter implements WebUserCurrentIdentityRepository {

    private final WebUserCurrentIdentityJpaRepository repo;

    public WebUserCurrentIdentityRepositoryJpaAdapter(WebUserCurrentIdentityJpaRepository repo) {
        this.repo = repo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends WebUserCurrentIdentity> S save(S entity) {
        return repo.save(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends WebUserCurrentIdentity> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<WebUserCurrentIdentity> findById(Long id) {
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
    public void delete(WebUserCurrentIdentity entity) {
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
    public Optional<WebUserCurrentIdentity> findByIdentityTypeAndIdentityHash(WebUserIdentityType identityType,
            String identityHash) {
        return repo.findByIdentityTypeAndIdentityHash(identityType, identityHash);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<WebUserCurrentIdentity> findByProviderCodeAndProviderSubject(String providerCode,
            String providerSubject) {
        return repo.findByProviderCodeAndProviderSubject(providerCode, providerSubject);
    }
}

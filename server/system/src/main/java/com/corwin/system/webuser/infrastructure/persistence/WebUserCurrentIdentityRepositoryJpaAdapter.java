package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserCurrentIdentity;
import com.corwin.system.webuser.domain.model.WebUserIdentityType;
import com.corwin.system.webuser.domain.repo.WebUserCurrentIdentityRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserCurrentIdentityRepositoryJpaAdapter implements WebUserCurrentIdentityRepository {

    private final WebUserCurrentIdentityJpaRepository repo;

    public WebUserCurrentIdentityRepositoryJpaAdapter(WebUserCurrentIdentityJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public <S extends WebUserCurrentIdentity> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends WebUserCurrentIdentity> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<WebUserCurrentIdentity> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(WebUserCurrentIdentity entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<WebUserCurrentIdentity> findByIdentityTypeAndIdentityHash(WebUserIdentityType identityType,
            String identityHash) {
        return repo.findByIdentityTypeAndIdentityHash(identityType, identityHash);
    }

    @Override
    public Optional<WebUserCurrentIdentity> findByProviderCodeAndProviderSubject(String providerCode,
            String providerSubject) {
        return repo.findByProviderCodeAndProviderSubject(providerCode, providerSubject);
    }
}

package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserCredential;
import com.corwin.system.webuser.domain.model.WebUserCredentialStatus;
import com.corwin.system.webuser.domain.model.WebUserCredentialType;
import com.corwin.system.webuser.domain.repo.WebUserCredentialRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserCredentialRepositoryJpaAdapter implements WebUserCredentialRepository {

    private final WebUserCredentialJpaRepository repo;

    public WebUserCredentialRepositoryJpaAdapter(WebUserCredentialJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public <S extends WebUserCredential> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends WebUserCredential> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<WebUserCredential> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(WebUserCredential entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<WebUserCredential> findFirstByUserIdAndCredentialTypeAndStatus(Long userId,
            WebUserCredentialType credentialType, WebUserCredentialStatus status) {
        return repo.findFirstByUserIdAndCredentialTypeAndStatus(userId, credentialType, status);
    }
}

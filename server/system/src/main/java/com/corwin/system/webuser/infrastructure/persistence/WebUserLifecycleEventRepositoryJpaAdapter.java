package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserLifecycleEvent;
import com.corwin.system.webuser.domain.repo.WebUserLifecycleEventRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation of {@link WebUserLifecycleEventRepository}.
 *
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserLifecycleEventRepositoryJpaAdapter implements WebUserLifecycleEventRepository {

    private final WebUserLifecycleEventJpaRepository repo;

    public WebUserLifecycleEventRepositoryJpaAdapter(WebUserLifecycleEventJpaRepository repo) {
        this.repo = repo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends WebUserLifecycleEvent> S save(S entity) {
        return repo.save(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends WebUserLifecycleEvent> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<WebUserLifecycleEvent> findById(Long id) {
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
    public void delete(WebUserLifecycleEvent entity) {
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
}

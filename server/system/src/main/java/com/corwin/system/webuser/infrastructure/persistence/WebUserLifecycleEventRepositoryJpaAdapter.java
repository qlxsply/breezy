package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserLifecycleEvent;
import com.corwin.system.webuser.domain.repo.WebUserLifecycleEventRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserLifecycleEventRepositoryJpaAdapter implements WebUserLifecycleEventRepository {

    private final WebUserLifecycleEventJpaRepository repo;

    public WebUserLifecycleEventRepositoryJpaAdapter(WebUserLifecycleEventJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public <S extends WebUserLifecycleEvent> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends WebUserLifecycleEvent> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<WebUserLifecycleEvent> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(WebUserLifecycleEvent entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }
}

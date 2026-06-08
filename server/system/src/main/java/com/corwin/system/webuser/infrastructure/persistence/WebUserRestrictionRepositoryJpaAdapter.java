package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserRestriction;
import com.corwin.system.webuser.domain.repo.WebUserRestrictionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
@Repository
public class WebUserRestrictionRepositoryJpaAdapter implements WebUserRestrictionRepository {

    private final WebUserRestrictionJpaRepository repo;

    public WebUserRestrictionRepositoryJpaAdapter(WebUserRestrictionJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public <S extends WebUserRestriction> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends WebUserRestriction> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<WebUserRestriction> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(WebUserRestriction entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<WebUserRestriction> findByUserId(Long userId) {
        return repo.findByUserId(userId);
    }
}

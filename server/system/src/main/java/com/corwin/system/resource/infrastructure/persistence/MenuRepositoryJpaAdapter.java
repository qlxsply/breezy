package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Menu;
import com.corwin.system.resource.domain.repo.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/7
 */
@Repository
@RequiredArgsConstructor
public class MenuRepositoryJpaAdapter implements MenuRepository {

    private final MenuJpaRepository repo;

    @Override
    public <S extends Menu> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Menu> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Menu> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Menu entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<Menu> findAll() {
        return repo.findAll();
    }
}

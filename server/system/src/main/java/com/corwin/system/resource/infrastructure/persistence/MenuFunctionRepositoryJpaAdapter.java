package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.MenuFunction;
import com.corwin.system.resource.domain.repo.MenuFunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/7
 */
@Repository
@RequiredArgsConstructor
public class MenuFunctionRepositoryJpaAdapter implements MenuFunctionRepository {

    private final MenuFunctionJpaRepository repo;

    @Override
    public <S extends MenuFunction> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends MenuFunction> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<MenuFunction> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(MenuFunction entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<MenuFunction> findAll() {
        return repo.findAll();
    }
}

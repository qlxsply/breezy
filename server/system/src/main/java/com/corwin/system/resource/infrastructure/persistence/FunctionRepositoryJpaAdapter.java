package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Function;
import com.corwin.system.resource.domain.repo.FunctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/5/7
 */
@Repository
@RequiredArgsConstructor
public class FunctionRepositoryJpaAdapter implements FunctionRepository {

    private final FunctionJpaRepository repo;

    @Override
    public <S extends Function> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Function> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Function> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Function entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<Function> findAll() {
        return repo.findAll();
    }
}

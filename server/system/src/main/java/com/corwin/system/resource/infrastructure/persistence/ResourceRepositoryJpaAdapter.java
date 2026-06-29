package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Resource;
import com.corwin.system.resource.domain.repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/6/29
 */
@Repository
@RequiredArgsConstructor
public class ResourceRepositoryJpaAdapter implements ResourceRepository {

    private final ResourceJpaRepository repo;

    @Override
    public <S extends Resource> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends Resource> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<Resource> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(Resource entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public List<Resource> findAll() {
        return repo.findAll();
    }

    @Override
    public List<Resource> findAllById(Collection<Long> ids) {
        return repo.findAllById(ids);
    }
}

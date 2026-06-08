package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.ItemCategory;
import com.corwin.clinic.domain.model.ItemSpu;
import com.corwin.clinic.domain.repo.ItemSpuRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Corwin 2026/2/8
 */
@Repository
@RequiredArgsConstructor
public class ItemSpuRepositoryJpaAdapter implements ItemSpuRepository {

    private final ItemSpuJpaRepository repo;


    @Override
    public <S extends ItemSpu> S save(S entity) {
        return repo.save(entity);
    }

    @Override
    public <S extends ItemSpu> List<S> saveAll(Iterable<S> entities) {
        return repo.saveAll(entities);
    }

    @Override
    public Optional<ItemSpu> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Override
    public void delete(ItemSpu entity) {
        repo.delete(entity);
        repo.flush();
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
        repo.flush();
    }

    @Override
    public Optional<ItemSpu> findByCategoryAndName(ItemCategory category, String name) {
        return repo.findByCategoryAndName(category, name);
    }

    @Override
    public List<ItemSpu> findByIdIn(List<Long> ids) {
        return repo.findAllById(ids);
    }
}

package com.corwin.framework.domain.repo;

import java.util.List;
import java.util.Optional;

/**
 * Base repository interface for domain-driven CRUD operations.
 * <p>
 * Abstracts common persistence operations (save, find, exists, delete)
 * following the repository pattern from domain-driven design.
 *
 * @param <T>  the entity type
 * @param <ID> the entity identifier type
 * @author Corwin 2026/1/29
 */
public interface DomainRepository<T, ID> {

    <S extends T> S save(S entity);

    <S extends T> List<S> saveAll(Iterable<S> entities);

    Optional<T> findById(ID id);

    boolean existsById(ID id);

    void delete(T entity);

    void deleteById(ID id);
}

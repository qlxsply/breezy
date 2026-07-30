package com.corwin.system.dict.infrastructure.persistence;

import com.corwin.system.dict.domain.model.DictType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DictType} entity.
 *
 * @author Corwin 2026/3/30
 */
public interface DictTypeJpaRepository extends JpaRepository<DictType, String> {

    /**
     * Finds a dictionary type by its unique code.
     */
    Optional<DictType> findByCode(String code);

    /**
     * Checks whether a dictionary type with the given code exists.
     */
    boolean existsByCode(String code);

    /**
     * Paginates dictionary types by code (case-insensitive contains).
     */
    Page<DictType> findByCodeContainingIgnoreCase(String code, Pageable pageable);

    /**
     * Paginates dictionary types by name (case-insensitive contains).
     */
    Page<DictType> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Paginates dictionary types by code or name (case-insensitive contains).
     */
    Page<DictType> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name,
            Pageable pageable);

    /**
     * Returns all dictionary types sorted by name ascending.
     */
    List<DictType> findAllByOrderByNameAsc();

}

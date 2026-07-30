package com.corwin.system.dict.domain.repo;

import com.corwin.system.dict.domain.model.DictType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.repo.DomainRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link DictType} domain aggregate.
 * <p>Provides domain-level data access operations for dictionary types.</p>
 *
 * @author Corwin 2026/3/15
 */
public interface DictTypeRepository extends DomainRepository<DictType, String> {

    /**
     * Finds a dictionary type by its unique code.
     */
    Optional<DictType> findByCode(String code);

    /**
     * Checks whether a dictionary type with the given code exists.
     */
    boolean existsByCode(String code);

    /**
     * Paginates dictionary types with optional code and name filtering.
     */
    PageData<DictType> page(String code, String name, PageSpec spec);

    /**
     * Returns all dictionary types sorted by name ascending.
     */
    List<DictType> findAllByOrderByNameAsc();

}

package com.corwin.system.dict.infrastructure.persistence;

import com.corwin.system.dict.domain.model.DictType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Corwin 2026/3/30
 */
public interface DictTypeJpaRepository extends JpaRepository<DictType, String> {

    Optional<DictType> findByCode(String code);

    boolean existsByCode(String code);

    Page<DictType> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name,
            Pageable pageable);

    List<DictType> findAllByOrderByNameAsc();

}

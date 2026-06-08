package com.corwin.clinic.infrastructure.persistence;

import com.corwin.clinic.domain.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * @author Corwin 2026/2/8
 */
public interface SupplierJpaRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByNameNorm(String nameNorm);

    @Query("""
            select s from Supplier s
            where (:nameLike is null or lower(s.name) like lower(concat('%', :nameLike, '%')))
              and (:enabled is null or s.enabled = :enabled)
            """)
    Page<Supplier> page(@Param("nameLike") String nameLike,
            @Param("enabled") Boolean enabled,
            Pageable pageable);
}

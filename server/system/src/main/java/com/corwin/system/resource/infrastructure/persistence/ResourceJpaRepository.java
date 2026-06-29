package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Corwin 2026/6/29
 */
public interface ResourceJpaRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByCode(String code);

    boolean existsByCode(String code);
}

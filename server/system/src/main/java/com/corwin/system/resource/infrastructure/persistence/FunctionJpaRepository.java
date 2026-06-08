package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Function;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/5/7
 */
public interface FunctionJpaRepository extends JpaRepository<Function, Long> {
}

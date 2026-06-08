package com.corwin.system.resource.infrastructure.persistence;

import com.corwin.system.resource.domain.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Corwin 2026/5/7
 */
public interface MenuJpaRepository extends JpaRepository<Menu, Long> {
}

package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link WebUserLifecycleEvent} entity.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserLifecycleEventJpaRepository
    extends JpaRepository<WebUserLifecycleEvent, Long> {}

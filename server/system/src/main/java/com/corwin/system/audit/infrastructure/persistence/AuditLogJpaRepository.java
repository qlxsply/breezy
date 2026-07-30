package com.corwin.system.audit.infrastructure.persistence;

import com.corwin.system.audit.domain.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link AuditLog} entity.
 * Provides basic CRUD operations delegated from the domain repository adapter.
 *
 * @author Corwin 2026/4/19
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLog, Long> {
}

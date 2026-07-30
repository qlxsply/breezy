package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserCredential;
import com.corwin.system.webuser.domain.model.WebUserCredentialStatus;
import com.corwin.system.webuser.domain.model.WebUserCredentialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link WebUserCredential} entity.
 *
 * @author Corwin 2026/5/11
 */
public interface WebUserCredentialJpaRepository extends JpaRepository<WebUserCredential, Long> {

    /**
     * Find the first credential matching user ID, credential type, and status.
     */
    Optional<WebUserCredential> findFirstByUserIdAndCredentialTypeAndStatus(Long userId,
            WebUserCredentialType credentialType, WebUserCredentialStatus status);
}

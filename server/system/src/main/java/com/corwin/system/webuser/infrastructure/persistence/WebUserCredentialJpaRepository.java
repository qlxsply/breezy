package com.corwin.system.webuser.infrastructure.persistence;

import com.corwin.system.webuser.domain.model.WebUserCredential;
import com.corwin.system.webuser.domain.model.WebUserCredentialStatus;
import com.corwin.system.webuser.domain.model.WebUserCredentialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserCredentialJpaRepository extends JpaRepository<WebUserCredential, Long> {

    Optional<WebUserCredential> findFirstByUserIdAndCredentialTypeAndStatus(Long userId,
            WebUserCredentialType credentialType, WebUserCredentialStatus status);
}

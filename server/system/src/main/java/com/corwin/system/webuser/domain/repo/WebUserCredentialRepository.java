package com.corwin.system.webuser.domain.repo;

import com.corwin.framework.domain.repo.DomainRepository;
import com.corwin.system.webuser.domain.model.WebUserCredential;
import com.corwin.system.webuser.domain.model.WebUserCredentialStatus;
import com.corwin.system.webuser.domain.model.WebUserCredentialType;

import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
public interface WebUserCredentialRepository extends DomainRepository<WebUserCredential, Long> {

    Optional<WebUserCredential> findFirstByUserIdAndCredentialTypeAndStatus(Long userId,
            WebUserCredentialType credentialType, WebUserCredentialStatus status);
}

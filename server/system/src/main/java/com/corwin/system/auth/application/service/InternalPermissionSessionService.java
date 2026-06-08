package com.corwin.system.auth.application.service;

import com.corwin.system.auth.domain.model.LoginSession;
import com.corwin.system.auth.domain.model.SessionStatus;
import com.corwin.system.auth.domain.repo.LoginSessionRepository;
import com.corwin.system.auth.infrastructure.security.AuthSessionCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author Corwin 2026/5/7
 */
@Service
@RequiredArgsConstructor
public class InternalPermissionSessionService {

    private final LoginSessionRepository loginSessionRepository;
    private final AuthSessionCacheService authSessionCacheService;

    public void kickOutActiveSessions(Collection<Long> userIds, String operator) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        String normalizedOperator = operator == null || operator.isBlank() ? "system" : operator.trim();
        for (Long userId : new LinkedHashSet<>(userIds)) {
            if (userId == null) {
                continue;
            }
            for (LoginSession session : loginSessionRepository.findByUserIdAndSessionStatus(userId, SessionStatus.ACTIVE)) {
                session.kickOut(normalizedOperator);
                loginSessionRepository.save(session);
                authSessionCacheService.delete(session.getTokenHash());
            }
        }
    }
}

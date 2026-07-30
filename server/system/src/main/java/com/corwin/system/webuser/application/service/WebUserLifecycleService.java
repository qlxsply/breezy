package com.corwin.system.webuser.application.service;

import com.corwin.framework.json.Json;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.webuser.domain.model.WebUserLifecycleEvent;
import com.corwin.system.webuser.domain.model.WebUserLifecycleEventType;
import com.corwin.system.webuser.domain.repo.WebUserLifecycleEventRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for recording lifecycle events (login, logout, disable, etc.) for web users.
 *
 * @author Corwin 2026/5/11
 */
@Service
public class WebUserLifecycleService {

    private final WebUserLifecycleEventRepository repository;

    public WebUserLifecycleService(WebUserLifecycleEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Record a lifecycle event for the given user.
     *
     * @param userId   the user ID
     * @param type     the event type
     * @param metadata additional event metadata as key-value pairs
     */
    public void record(Long userId, WebUserLifecycleEventType type, Map<String, Object> metadata) {
        repository.save(new WebUserLifecycleEvent(userId, type, operatorType(), operatorId(),
                metadata == null || metadata.isEmpty() ? null : Json.toStr(metadata)));
    }

    private String operatorType() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        if (principal == null || principal.userType() == null) {
            return "SYSTEM";
        }
        return principal.userType().name();
    }

    private String operatorId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        if (principal == null) {
            return null;
        }
        if (principal.userId() != null) {
            return String.valueOf(principal.userId());
        }
        return principal.username();
    }
}

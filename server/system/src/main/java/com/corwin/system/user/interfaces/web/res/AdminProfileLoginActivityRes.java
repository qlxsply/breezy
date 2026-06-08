package com.corwin.system.user.interfaces.web.res;

import com.corwin.system.auth.domain.model.LoginEventType;

import java.time.Instant;

/**
 * @author Corwin 2026/6/4
 */
public record AdminProfileLoginActivityRes(
        String id,
        LoginEventType eventType,
        boolean success,
        String loginIp,
        String remark,
        Instant occurredAt
) {
}

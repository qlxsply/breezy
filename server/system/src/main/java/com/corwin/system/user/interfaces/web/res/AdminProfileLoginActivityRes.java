package com.corwin.system.user.interfaces.web.res;

import com.corwin.system.auth.domain.model.LoginEventType;
import java.time.Instant;

/**
 * Response DTO for a single admin login activity record.
 *
 * @author Corwin 2026/6/4
 */
public record AdminProfileLoginActivityRes(
    String id,
    LoginEventType eventType,
    boolean success,
    String loginIp,
    String remark,
    Instant occurredAt) {}

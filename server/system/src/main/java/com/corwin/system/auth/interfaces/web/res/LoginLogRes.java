package com.corwin.system.auth.interfaces.web.res;

import com.corwin.framework.json.JsonLongString;
import com.corwin.system.auth.domain.model.LoginEventType;
import java.time.Instant;

/**
 * Response DTO for a login log entry.
 *
 * @author Corwin 2026/1/23
 */
public record LoginLogRes(
    @JsonLongString Long id,
    @JsonLongString Long userId,
    String username,
    LoginEventType eventType,
    boolean success,
    String loginIp,
    String failureReason,
    @JsonLongString Long sessionId,
    @JsonLongString Long operatorId,
    Instant occurredAt,
    String remark) {}

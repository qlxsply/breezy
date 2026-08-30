package com.corwin.system.user.application.view;

import com.corwin.system.auth.domain.model.LoginEventType;
import java.time.Instant;

/**
 * View object representing a single login activity record for an admin profile.
 *
 * @author Corwin 2026/6/4
 */
public record AdminProfileLoginActivityView(
    Long id,
    LoginEventType eventType,
    boolean success,
    String loginIp,
    String remark,
    Instant occurredAt) {}

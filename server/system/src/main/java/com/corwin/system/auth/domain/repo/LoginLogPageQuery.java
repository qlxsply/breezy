package com.corwin.system.auth.domain.repo;

import com.corwin.system.auth.domain.model.LoginEventType;
import java.time.Instant;
import java.util.List;

/**
 * Page query criteria for filtering login log entries.
 *
 * @author Corwin 2026/4/15
 */
public record LoginLogPageQuery(
    String userAccount,
    Instant startAt,
    boolean startInclusive,
    Instant endAt,
    List<LoginEventType> eventTypes) {}

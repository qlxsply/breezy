package com.corwin.system.auth.domain.repo;

import com.corwin.system.auth.domain.model.LoginEventType;

import java.util.List;
import java.time.Instant;

/**
 * 登录日志分页查询条件。
 *
 * @author Corwin 2026/4/15
 */
public record LoginLogPageQuery(
        String userAccount,
        Instant startAt,
        boolean startInclusive,
        Instant endAt,
        List<LoginEventType> eventTypes
) {
}

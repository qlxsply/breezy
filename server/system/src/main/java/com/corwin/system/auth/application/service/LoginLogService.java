package com.corwin.system.auth.application.service;

import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.auth.domain.model.LoginEventType;
import com.corwin.system.auth.domain.repo.LoginLogPageQuery;
import com.corwin.system.auth.domain.repo.LoginLogRepository;
import com.corwin.system.user.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/1/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public void logLoginSuccess(User user) {
        if (user == null) {
            return;
        }
        save(LoginEventType.LOGIN_SUCCESS, true, user.getId(), user.getUsername(), null);
    }

    public void logLoginFailure(String account, String message) {
        save(LoginEventType.LOGIN_FAILURE, false, null, account, message);
    }

    public void logLogoutSuccess(Long userId, String account) {
        save(LoginEventType.LOGOUT, true, userId, account, null);
    }

    public void logLogoutFailure(Long userId, String account, String message) {
        save(LoginEventType.LOGOUT, false, userId, account, message);
    }

    public PageData<LoginEvent> page(String userAccount, Instant startAt, Instant endAt, PageSpec spec) {
        String account = StrUtil.trimToNull(userAccount);
        PageSpec resolved = withDefaultSort(spec);
        boolean hasStart = startAt != null;
        boolean hasEnd = endAt != null;
        LoginLogPageQuery query = new LoginLogPageQuery(account, startAt, hasStart && hasEnd, endAt, List.of());
        return loginLogRepository.pageByQuery(query, resolved);
    }

    public PageData<LoginEvent> pageOwnLoginActivities(String userAccount, PageSpec spec) {
        String account = StrUtil.trimToNull(userAccount);
        PageSpec resolved = withDefaultSort(spec);
        LoginLogPageQuery query = new LoginLogPageQuery(account, null, false, null,
                List.of(LoginEventType.LOGIN_SUCCESS, LoginEventType.LOGOUT));
        return loginLogRepository.pageByQuery(query, resolved);
    }

    public List<LoginEvent> listRecentLoginActivities(String userAccount, int limit) {
        String account = StrUtil.trimToNull(userAccount);
        if (account == null) {
            return List.of();
        }
        int safeLimit = Math.clamp(limit, 1, 50);
        PageSpec spec = new PageSpec(1, Math.max(safeLimit, 50),
                List.of(new SortSpec("occurred_at", SortDirection.DESC)));
        return pageOwnLoginActivities(account, spec).elements().stream().limit(safeLimit).toList();
    }

    private void save(LoginEventType event, boolean success, Long userId, String account, String message) {
        try {
            LoginEvent logEntry = new LoginEvent(userId, StrUtil.trimToNull(account), event, success,
                    CtxUtil.getClientIp(), StrUtil.trimToNull(message), buildRemark());
            loginLogRepository.save(logEntry);
        } catch (RuntimeException ex) {
            log.warn("Failed to save login log: {}", ex.getMessage());
        }
    }

    private String readUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return StrUtil.trimToNull(request.getHeader("User-Agent"));
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    private PageSpec withDefaultSort(PageSpec spec) {
        PageSpec resolved = (spec == null) ? PageSpec.of(null, null, List.of()) : spec;
        if (!resolved.sorts().isEmpty()) {
            return resolved;
        }
        return new PageSpec(resolved.pageNo(), resolved.pageSize(),
                List.of(new SortSpec("occurred_at", SortDirection.DESC)));
    }

    private String buildRemark() {
        String userAgent = readUserAgent();
        String client = StrUtil.trimToNull(CtxUtil.getClient());
        if (userAgent == null && client == null) {
            return null;
        }
        if (userAgent == null) {
            return client;
        }
        if (client == null) {
            return userAgent;
        }
        return client + " | " + userAgent;
    }
}

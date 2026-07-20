package com.corwin.system.user.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.service.LoginLogService;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.auth.domain.model.LoginEventType;
import com.corwin.system.user.application.command.UpdateAdminProfileCommand;
import com.corwin.system.user.application.view.AdminProfileLoginActivityView;
import com.corwin.system.user.application.view.AdminProfileView;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Corwin 2026/6/4
 */
@Service
@RequiredArgsConstructor
public class AdminProfileAppService {

    private final UserRepository userRepository;
    private final LoginLogService loginLogService;

    @Transactional(readOnly = true)
    public AdminProfileView currentProfile() {
        User user = currentInternalUser();
        return toView(user);
    }

    @Transactional
    public AdminProfileView updateMyProfile(UpdateAdminProfileCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        User user = currentInternalUser();
        String nickname = normalizeNickname(cmd.nickname(), user.getUsername());
        user.updateNickname(nickname, operator());
        user = userRepository.save(user);
        return toView(user);
    }

    @Transactional(readOnly = true)
    public String currentUsername() {
        return currentInternalUser().getUsername();
    }

    private AdminProfileView toView(User user) {
        List<AdminProfileLoginActivityView> recentActivities = loginLogService
                .listRecentLoginActivities(user.getUsername(), 20).stream()
                .filter(event -> event.getEventType() == LoginEventType.LOGIN_SUCCESS
                        || event.getEventType() == LoginEventType.LOGOUT)
                .map(AdminProfileAppService::toActivityView)
                .toList();
        return new AdminProfileView(user.getId(), user.getUsername(), user.getNickname(), user.getUserType(),
                user.getUserStatus().name(), user.isMustChangePassword(), user.getLastPasswordChangedAt(),
                user.getCreatedAt(), user.getUpdatedAt(), recentActivities);
    }

    private static AdminProfileLoginActivityView toActivityView(LoginEvent event) {
        return new AdminProfileLoginActivityView(event.getId(), event.getEventType(), event.isSuccess(),
                event.getLoginIp(), event.getRemark(), event.getOccurredAt());
    }

    private User currentInternalUser() {
        var principal = CtxUtil.getPrincipal();
        Long userId = principal == null ? null : principal.userId();
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        User user = userRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        BizAssert.state(user.getUserType() == UserType.ADMIN || DefaultUser.isSystemUser(user.getId()),
                BaseError.FORBIDDEN);
        return user;
    }

    private String normalizeNickname(String nickname, String fallback) {
        if (nickname == null || nickname.isBlank()) {
            return fallback;
        }
        return nickname.trim();
    }

    private String operator() {
        var principal = CtxUtil.getPrincipal();
        if (principal == null || principal.username() == null || principal.username().isBlank()) {
            return "system";
        }
        return principal.username().trim();
    }
}

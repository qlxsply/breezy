package com.corwin.system.user.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.service.LoginLogService;
import com.corwin.system.auth.domain.model.LoginEvent;
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
 * Application service for admin profile operations including
 * profile retrieval, update, and login activity queries.
 *
 * @author Corwin 2026/6/4
 */
@Service
@RequiredArgsConstructor
public class AdminProfileAppService {

    private final UserRepository userRepository;
    private final LoginLogService loginLogService;

    /**
     * Returns the profile of the currently authenticated admin user,
     * including recent login activity.
     *
     * @return the admin profile view
     */
    @Transactional(readOnly = true)
    public AdminProfileView currentProfile() {
        User user = currentInternalUser();
        return toView(user);
    }

    /**
     * Updates the nickname of the currently authenticated admin user.
     *
     * @param cmd the update command containing the new nickname
     * @return the updated admin profile view
     */
    @Transactional
    public AdminProfileView updateMyProfile(UpdateAdminProfileCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        User user = currentInternalUser();
        String nickname = normalizeNickname(cmd.nickname(), user.getUsername());
        user.updateNickname(nickname, operator());
        user = userRepository.save(user);
        return toView(user);
    }

    /**
     * Returns the username of the currently authenticated admin user.
     *
     * @return the current admin username
     */
    @Transactional(readOnly = true)
    public String currentUsername() {
        return currentInternalUser().getUsername();
    }

    private AdminProfileView toView(User user) {
        List<AdminProfileLoginActivityView> recentActivities = loginLogService.listRecentLoginActivities(
                user.getUsername(), 20).stream().map(AdminProfileAppService::toActivityView).toList();
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

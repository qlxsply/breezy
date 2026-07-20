package com.corwin.system.user.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.application.service.PasswordPolicyService;
import com.corwin.system.role.domain.repo.RoleRepository;
import com.corwin.system.user.application.command.BatchUpdateUserStatusCommand;
import com.corwin.system.user.application.command.BatchUserIdsCommand;
import com.corwin.system.user.application.command.CreateUserCommand;
import com.corwin.system.user.application.command.UpdateUserCommand;
import com.corwin.system.user.domain.model.DefaultUser;
import com.corwin.system.user.domain.model.User;
import com.corwin.system.user.domain.model.UserRole;
import com.corwin.system.user.domain.model.UserStatus;
import com.corwin.system.user.domain.repo.UserRepository;
import com.corwin.system.user.domain.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Corwin 2026/1/22
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final String DEFAULT_RESET_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public List<User> list() {
        return userRepository.findAllByOrderByIdAsc();
    }

    public PageData<User> page(UserStatus status, String usernameLike, PageSpec spec) {
        boolean hasAccount = usernameLike != null && !usernameLike.isBlank();
        String account = hasAccount ? usernameLike.trim() : null;

        if (status != null && hasAccount) {
            return userRepository.findByStatusAndUsernameContainingIgnoreCase(status, account, spec);
        }
        if (status != null) {
            return userRepository.findByStatus(status, spec);
        }
        if (hasAccount) {
            return userRepository.findByUsernameContainingIgnoreCase(account, spec);
        }
        return userRepository.findAll(spec);
    }

    @Transactional
    public User create(CreateUserCommand cmd) {
        String username = normalizeUsername(cmd.username());
        String nickname = normalizeNickname(cmd.nickname());

        passwordPolicyService.validate(cmd.password());
        BizAssert.state(!userRepository.existsByUsername(username), BaseError.CONFLICT);

        UserStatus status = UserStatus.ENABLED;
        String hash = BCrypt.hashpw(cmd.password(), BCrypt.gensalt());

        User user = new User(UserType.ADMIN, username, nickname, hash, "bcrypt", status, false,
                HighDate.mockInstant(), operator());
        User saved = userRepository.save(user);
        createUserRoles(saved.getId(), cmd.roleIds());
        return saved;
    }

    @Transactional
    public User update(Long id, UpdateUserCommand cmd) {
        BizAssert.state(!DefaultUser.isReserved(id), BaseError.FORBIDDEN);
        User user = userRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        String nickname = normalizeNickname(cmd.nickname());
        UserStatus nextStatus = cmd.status() == null ? user.getUserStatus() : cmd.status();
        user.updateNickname(nickname, operator());
        user.updateStatus(nextStatus, operator());
        return userRepository.save(user);
    }

    @Transactional
    public void resetPassword(Long id) {
        BizAssert.state(!DefaultUser.isReserved(id), BaseError.FORBIDDEN);
        User user = userRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
        String hash = BCrypt.hashpw(DEFAULT_RESET_PASSWORD, BCrypt.gensalt());
        user.resetPassword(hash, "", operator());
        userRepository.save(user);
    }

    @Transactional
    public void batchUpdateStatus(BatchUpdateUserStatusCommand cmd) {
        BizAssert.notNull(cmd, BaseError.MISSING_PARAMETER);
        BizAssert.notNull(cmd.status(), BaseError.MISSING_PARAMETER);
        for (Long userId : normalizeUserIds(cmd.userIds())) {
            BizAssert.state(!DefaultUser.isReserved(userId), BaseError.FORBIDDEN);
            User user = userRepository.findById(userId).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
            user.updateStatus(cmd.status(), operator());
            userRepository.save(user);
        }
    }

    @Transactional
    public void batchResetPassword(BatchUserIdsCommand cmd) {
        BizAssert.notNull(cmd, BaseError.MISSING_PARAMETER);
        for (Long userId : normalizeUserIds(cmd.userIds())) {
            resetPassword(userId);
        }
    }

    public User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    @Transactional
    public void delete(Long id) {
        BizAssert.state(!DefaultUser.isReserved(id), BaseError.FORBIDDEN);
        User user = get(id);
        userRoleRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    @Transactional
    public void batchDelete(BatchUserIdsCommand cmd) {
        BizAssert.notNull(cmd, BaseError.MISSING_PARAMETER);
        for (Long userId : normalizeUserIds(cmd.userIds())) {
            delete(userId);
        }
    }


    private String normalizeUsername(String username) {
        BizAssert.notBlank(username, BaseError.MISSING_PARAMETER);
        return username.trim();
    }

    private String normalizeNickname(String nickname) {
        BizAssert.notBlank(nickname, BaseError.MISSING_PARAMETER);
        return nickname.trim();
    }

    private void createUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Long operatorId = operatorId();
        List<UserRole> next = new ArrayList<>();
        for (Long roleId : new LinkedHashSet<>(roleIds)) {
            if (roleId == null || !roleRepository.existsById(roleId)) {
                continue;
            }
            next.add(new UserRole(userId, roleId, operatorId));
        }
        userRoleRepository.saveAll(next);
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        BizAssert.notEmpty(userIds, BaseError.MISSING_PARAMETER);
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                normalized.add(userId);
            }
        }
        BizAssert.notEmpty(normalized, BaseError.MISSING_PARAMETER);
        return List.copyOf(normalized);
    }

    private String operator() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        String operator = principal == null ? null : principal.username();
        BizAssert.notBlank(operator, BaseError.FORBIDDEN);
        return operator.trim();
    }

    private Long operatorId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        Long operatorId = principal == null ? null : principal.userId();
        BizAssert.notNull(operatorId, BaseError.FORBIDDEN);
        return operatorId;
    }
}

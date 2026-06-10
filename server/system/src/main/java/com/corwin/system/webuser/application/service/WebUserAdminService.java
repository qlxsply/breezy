package com.corwin.system.webuser.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.webuser.application.command.UpdateWebUserCommand;
import com.corwin.system.webuser.application.view.WebUserAdminView;
import com.corwin.system.webuser.domain.model.*;
import com.corwin.system.webuser.domain.repo.WebUserIdentityRepository;
import com.corwin.system.webuser.domain.repo.WebUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author Corwin 2026/5/11
 */
@Service
public class WebUserAdminService {

    private final WebUserRepository webUserRepository;
    private final WebUserIdentityRepository webUserIdentityRepository;
    private final WebUserLifecycleService webUserLifecycleService;

    public WebUserAdminService(WebUserRepository webUserRepository, WebUserIdentityRepository webUserIdentityRepository,
                               WebUserLifecycleService webUserLifecycleService) {
        this.webUserRepository = webUserRepository;
        this.webUserIdentityRepository = webUserIdentityRepository;
        this.webUserLifecycleService = webUserLifecycleService;
    }

    public PageData<WebUserAdminView> page(String keyword, String status, PageSpec spec) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        WebUserStatus normalizedStatus = parseStatus(status);
        PageData<WebUser> page;
        if (normalizedStatus != null && normalizedKeyword != null) {
            page = webUserRepository.findByStatusAndKeyword(normalizedStatus, normalizedKeyword, spec);
        } else if (normalizedStatus != null) {
            page = webUserRepository.findByStatus(normalizedStatus, spec);
        } else if (normalizedKeyword != null) {
            page = webUserRepository.findByKeyword(normalizedKeyword, spec);
        } else {
            page = webUserRepository.findAll(spec);
        }
        return new PageData<>(page.pageNo(), page.pageSize(), page.numberOfElements(), page.totalPages(),
                page.totalElements(), page.elements().stream().map(this::toView).toList());
    }

    public WebUserAdminView get(Long id) {
        return toView(requireUser(id));
    }

    @Transactional
    public WebUserAdminView update(Long id, UpdateWebUserCommand cmd) {
        BizAssert.notNull(cmd, BaseError.INVALID_PARAMETER);
        WebUser user = requireUser(id);
        WebUserStatus targetStatus = parseStatus(cmd.status());
        if (targetStatus == WebUserStatus.DISABLED && user.getStatus() != WebUserStatus.DISABLED) {
            user.disable("admin disabled", operator());
            webUserLifecycleService.record(user.getId(), WebUserLifecycleEventType.DISABLED, java.util.Map.of());
        } else if (targetStatus == WebUserStatus.ACTIVE && user.getStatus() == WebUserStatus.DISABLED) {
            user.enable(operator());
            webUserLifecycleService.record(user.getId(), WebUserLifecycleEventType.ENABLED, java.util.Map.of());
        }
        return toView(webUserRepository.save(user));
    }

    private WebUser requireUser(Long id) {
        return webUserRepository.findById(id).orElseThrow(() -> new BizException(BaseError.NOT_FOUND));
    }

    private WebUserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return WebUserStatus.valueOf(status.trim());
        } catch (IllegalArgumentException ex) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }

    private WebUserAdminView toView(WebUser user) {
        return new WebUserAdminView(user.getId(), resolveAccount(user), user.getNickname(), UserType.EXTERNAL,
                user.getStatus().name(), user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }

    private String resolveAccount(WebUser user) {
        Long primaryIdentityId = user.getPrimaryIdentityId();
        if (primaryIdentityId != null) {
            Optional<WebUserIdentity> primary = webUserIdentityRepository.findById(primaryIdentityId);
            if (primary.isPresent()) {
                return primary.get().getIdentityValue();
            }
        }
        return webUserIdentityRepository.findFirstByUserIdAndIdentityType(user.getId(), WebUserIdentityType.USERNAME)
                                        .map(WebUserIdentity::getIdentityValue).orElse(String.valueOf(user.getId()));
    }

    private String operator() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        String operator = principal == null ? null : principal.username();
        BizAssert.notBlank(operator, BaseError.FORBIDDEN);
        return operator.trim();
    }
}

package com.corwin.system.user.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.auth.application.service.LoginLogService;
import com.corwin.system.auth.domain.model.LoginEvent;
import com.corwin.system.auth.published.Authenticated;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.user.application.command.UpdateAdminProfileCommand;
import com.corwin.system.user.application.service.AdminProfileAppService;
import com.corwin.system.user.application.view.AdminProfileLoginActivityView;
import com.corwin.system.user.application.view.AdminProfileView;
import com.corwin.system.user.interfaces.web.req.AdminProfileLoginActivityPageReq;
import com.corwin.system.user.interfaces.web.req.UpdateAdminProfileReq;
import com.corwin.system.user.interfaces.web.res.AdminProfileLoginActivityRes;
import com.corwin.system.user.interfaces.web.res.AdminProfileRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for admin profile operations including profile retrieval, login activity
 * pagination, and profile update.
 *
 * @author Corwin 2026/6/4
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

  private final AdminProfileAppService adminProfileAppService;
  private final LoginLogService loginLogService;

  /**
   * Returns the profile of the currently authenticated admin user.
   *
   * @return the admin profile with recent login activities
   */
  @GetMapping
  @Authenticated(userType = UserType.ADMIN)
  public ApiResponse<AdminProfileRes> currentProfile() {
    return ApiResponse.ok(toRes(adminProfileAppService.currentProfile()));
  }

  /**
   * Paginates the login activities for the current admin user.
   *
   * @param req the page request
   * @return a paginated result of login activities
   */
  @PostMapping("/login-activities")
  @Authenticated(userType = UserType.ADMIN)
  public ApiResponse<PageResult<AdminProfileLoginActivityRes>> pageLoginActivities(
      @RequestBody AdminProfileLoginActivityPageReq req) {
    String username = adminProfileAppService.currentUsername();
    var page =
        loginLogService.pageOwnLoginActivities(username, PageSpecFactory.of(req.page(), null));
    return ApiResponse.ok(PageResult.of(page, AdminProfileController::toActivityRes));
  }

  /**
   * Updates the nickname of the currently authenticated admin user.
   *
   * @param req the update request containing the new nickname
   * @return the updated admin profile
   */
  @PutMapping
  @Authenticated(userType = UserType.ADMIN)
  public ApiResponse<AdminProfileRes> updateMyProfile(@RequestBody UpdateAdminProfileReq req) {
    return ApiResponse.ok(
        toRes(
            adminProfileAppService.updateMyProfile(new UpdateAdminProfileCommand(req.nickname()))));
  }

  private static AdminProfileRes toRes(AdminProfileView view) {
    return new AdminProfileRes(
        String.valueOf(view.id()),
        view.username(),
        view.nickname(),
        view.userType(),
        view.status(),
        view.mustChangePassword(),
        view.lastPasswordChangedAt(),
        view.createdAt(),
        view.updatedAt(),
        view.recentActivities().stream().map(AdminProfileController::toActivityRes).toList());
  }

  private static AdminProfileLoginActivityRes toActivityRes(AdminProfileLoginActivityView view) {
    return new AdminProfileLoginActivityRes(
        String.valueOf(view.id()),
        view.eventType(),
        view.success(),
        view.loginIp(),
        view.remark(),
        view.occurredAt());
  }

  private static AdminProfileLoginActivityRes toActivityRes(LoginEvent event) {
    return new AdminProfileLoginActivityRes(
        String.valueOf(event.getId()),
        event.getEventType(),
        event.isSuccess(),
        event.getLoginIp(),
        event.getRemark(),
        event.getOccurredAt());
  }
}

package com.corwin.system.auth.application.service;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.Ctx;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.system.auth.published.RunAsService;
import com.corwin.system.user.domain.model.DefaultUser;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link RunAsService} that supports executing code under system,
 * scheduler, event, or custom principals by manipulating the request-level security context.
 *
 * @author Corwin 2026/4/19
 */
@Service
public class DefaultRunAsService implements RunAsService {

  @Override
  public void runAsSystem(Runnable action) {
    callAs(
        systemPrincipal(DefaultUser.SYSTEM),
        () -> {
          action.run();
          return null;
        });
  }

  @Override
  public void runAsScheduler(Runnable action) {
    callAs(
        systemPrincipal(DefaultUser.SCHEDULER),
        () -> {
          action.run();
          return null;
        });
  }

  @Override
  public void runAsEvent(Runnable action) {
    callAs(
        systemPrincipal(DefaultUser.EVENT),
        () -> {
          action.run();
          return null;
        });
  }

  @Override
  public <T> T callAsSystem(Supplier<T> action) {
    return callAs(systemPrincipal(DefaultUser.SYSTEM), action);
  }

  @Override
  public <T> T callAs(AuthPrincipal principal, Supplier<T> action) {
    Ctx snapshot = CtxUtil.snapshot();
    try {
      bindPrincipal(principal);
      return action.get();
    } finally {
      CtxUtil.restore(snapshot);
    }
  }

  private AuthPrincipal systemPrincipal(DefaultUser defaultUser) {
    return new AuthPrincipal(
        defaultUser.id(),
        defaultUser.account(),
        UserType.SYSTEM,
        DefaultUser.isAdmin(defaultUser.id()),
        Set.of());
  }

  private void bindPrincipal(AuthPrincipal principal) {
    CtxUtil.setPrincipal(principal);
  }
}

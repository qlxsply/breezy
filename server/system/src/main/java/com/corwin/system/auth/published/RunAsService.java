package com.corwin.system.auth.published;

import com.corwin.framework.web.auth.AuthPrincipal;

import java.util.function.Supplier;

/**
 * @author Corwin 2026/4/19
 */
public interface RunAsService {

    void runAsSystem(Runnable action);

    void runAsScheduler(Runnable action);

    void runAsEvent(Runnable action);

    <T> T callAsSystem(Supplier<T> action);

    <T> T callAs(AuthPrincipal principal, Supplier<T> action);
}

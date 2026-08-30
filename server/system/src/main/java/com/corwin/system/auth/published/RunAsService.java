package com.corwin.system.auth.published;

import com.corwin.framework.web.auth.AuthPrincipal;
import java.util.function.Supplier;

/**
 * Service interface for executing code under a different security principal, such as system,
 * scheduler, or event actors.
 *
 * @author Corwin 2026/4/19
 */
public interface RunAsService {

  /**
   * Executes the given action as the system principal.
   *
   * @param action the runnable to execute
   */
  void runAsSystem(Runnable action);

  /**
   * Executes the given action as the scheduler principal.
   *
   * @param action the runnable to execute
   */
  void runAsScheduler(Runnable action);

  /**
   * Executes the given action as the event principal.
   *
   * @param action the runnable to execute
   */
  void runAsEvent(Runnable action);

  /**
   * Calls the given supplier and returns a result as the system principal.
   *
   * @param <T> the return type
   * @param action the supplier to execute
   * @return the result
   */
  <T> T callAsSystem(Supplier<T> action);

  /**
   * Calls the given supplier under a custom principal.
   *
   * @param <T> the return type
   * @param principal the principal to run as
   * @param action the supplier to execute
   * @return the result
   */
  <T> T callAs(AuthPrincipal principal, Supplier<T> action);
}

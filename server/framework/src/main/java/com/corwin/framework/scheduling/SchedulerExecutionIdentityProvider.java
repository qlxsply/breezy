package com.corwin.framework.scheduling;

/**
 * Strategy interface providing the {@link SchedulerExecutionIdentity} for scheduled tasks.
 * <p>
 * Implementations typically look up a dedicated system user or configuration.
 *
 * @author Corwin 2026/3/23
 */
public interface SchedulerExecutionIdentityProvider {

    SchedulerExecutionIdentity identity();

}

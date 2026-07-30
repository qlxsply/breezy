package com.corwin.system.scheduler.infrastructure.scheduling;

import com.corwin.framework.scheduling.SchedulerExecutionIdentity;
import com.corwin.framework.scheduling.SchedulerExecutionIdentityProvider;
import com.corwin.system.user.domain.model.DefaultUser;
import org.springframework.stereotype.Component;

/**
 * Provides the scheduler execution identity using the default scheduler user.
 *
 * @author Corwin 2026/3/23
 */
@Component
public class BreezySchedulerExecutionIdentityProvider implements SchedulerExecutionIdentityProvider {

    /** Returns the identity of the built-in scheduler user. */
    @Override
    public SchedulerExecutionIdentity identity() {
        return new SchedulerExecutionIdentity(DefaultUser.SCHEDULER.id(), DefaultUser.SCHEDULER.account(),
                DefaultUser.SCHEDULER.userType());
    }

}

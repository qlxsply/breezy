package com.corwin.framework.scheduling;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.ctx.TraceGenerator;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.Set;

/**
 * {@link TaskDecorator} that initializes a fresh system context for {@code @Scheduled} tasks.
 * <p>
 * Unlike {@link com.corwin.framework.concurrency.ContextCopyingTaskDecorator}, this decorator
 * does <b>not</b> copy the HTTP request context. Instead it creates a new trace,
 * resolves the scheduler execution identity, and binds MDC
 * — treating every scheduled execution as its own system-level entry point.
 * Context is cleaned up after execution to prevent thread-pool pollution.
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/23
 */
public class SchedulerTaskDecorator implements TaskDecorator {

    private final SchedulerExecutionIdentityProvider identityProvider;

    public SchedulerTaskDecorator(SchedulerExecutionIdentityProvider identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Override
    public Runnable decorate(@NonNull Runnable runnable) {
        return () -> {
            String traceId = TraceGenerator.newTraceId();
            String spanId = TraceGenerator.newSpanId();

            try {
                // 1. Initialize the scheduled-task context
                CtxUtil.schedulerInitTrace(traceId);
                CtxUtil.setSpanInfo(spanId, null);
                SchedulerExecutionIdentity identity = Objects.requireNonNull(identityProvider.identity(),
                        "scheduler execution identity required");
                Long userId = identity.userId();
                String username = identity.username();
                UserType userType = identity.userType();
                CtxUtil.setPrincipal(new AuthPrincipal(userId, username, userType, false, Set.of()));

                // 2. Bind MDC
                CtxUtil.bindMdc();

                // 3. Execute the task
                runnable.run();
            } finally {
                // 4. Clear context to prevent thread-reuse pollution
                CtxUtil.reset();
            }
        };
    }
}

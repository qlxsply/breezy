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
 * 定时任务上下文装饰器。
 *
 * <p>用途：</p>
 * <ul>
 *     <li>为 {@code @Scheduled} 任务创建独立的线程上下文</li>
 *     <li>不复制 HTTP 请求上下文</li>
 *     <li>通过 {@link SchedulerExecutionIdentityProvider} 解析调度执行用户</li>
 *     <li>自动绑定 MDC，保证日志格式统一</li>
 * </ul>
 *
 * <p>设计原则：</p>
 * <ul>
 *     <li>定时任务不属于任何外部请求，因此不应继承请求线程中的 Ctx</li>
 *     <li>每次任务触发都视为一个新的“系统内部执行入口”</li>
 *     <li>执行完成后必须清理上下文，避免线程复用污染</li>
 * </ul>
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
                // 1. 初始化调度任务上下文
                CtxUtil.schedulerInitTrace(traceId);
                CtxUtil.setSpanInfo(spanId, null);
                SchedulerExecutionIdentity identity = Objects.requireNonNull(identityProvider.identity(),
                        "scheduler execution identity required");
                Long userId = identity.userId();
                String username = identity.username();
                UserType userType = identity.userType();
                CtxUtil.setPrincipal(new AuthPrincipal(userId, username, userType, false, Set.of()));

                // 2. 绑定 MDC
                CtxUtil.bindMdc();

                // 3. 执行任务
                runnable.run();
            } finally {
                // 4. 清理上下文，防止线程复用污染
                CtxUtil.reset();
            }
        };
    }
}

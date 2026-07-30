package com.corwin.framework.event.model;

import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.Ctx;

/**
 * Frozen snapshot of the request context captured at publish time.
 * <p>
 * Key fields (trace ID, user principal, client info) are stored so that
 * {@code ConsumeContextBinder} can restore them in the consumer thread,
 * enabling cross-async-boundary traceability.
 * <p>
 * Uses a fixed-field whitelist strategy — no dynamic properties are propagated
 * — to control serialisation size and evolution risk.
 *
 * @author Corwin 2026/3/31
 */
public record EventCtxSnapshot(
        long requestStartTs,
        long requestStartNano,
        String traceId,
        String spanId,
        String parentSpanId,
        String client,
        String clientIp,
        AuthPrincipal principal,
        String tokenHash
) {

    /**
     * 从当前上下文对象提取可传输字段，生成快照。
     *
     * @param ctx 当前线程上下文；为空时返回空快照
     * @return 用于事件信封传输的上下文快照
     */
    public static EventCtxSnapshot from(Ctx ctx) {
        if (ctx == null) {
            return empty();
        }
        return new EventCtxSnapshot(ctx.getRequestStartTs(), ctx.getRequestStartNano(), ctx.getTraceId(),
                ctx.getSpanId(), ctx.getParentSpanId(), ctx.getClient(), ctx.getClientIp(), ctx.getPrincipal(),
                ctx.getTokenHash());
    }

    /**
     * 创建空上下文快照。
     *
     * @return 所有字段均为默认值的快照对象
     */
    public static EventCtxSnapshot empty() {
        return new EventCtxSnapshot(0L, 0L, null, null, null, null, null, null, null);
    }

    /**
     * 将快照还原为运行时上下文对象。
     * <p>
     * 该方法仅负责字段映射，不直接绑定到线程，真正绑定动作由上下文绑定器完成。
     *
     * @return 可用于恢复线程上下文的 {@link Ctx} 实例
     */
    public Ctx toCtx() {
        Ctx ctx = new Ctx();
        ctx.setRequestStartTs(requestStartTs);
        ctx.setRequestStartNano(requestStartNano);
        ctx.setTraceId(traceId);
        ctx.setSpanId(spanId);
        ctx.setParentSpanId(parentSpanId);
        ctx.setClient(client);
        ctx.setClientIp(clientIp);
        ctx.setPrincipal(principal);
        ctx.setTokenHash(tokenHash);
        return ctx;
    }
}

package com.corwin.framework.event.model;

import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.Ctx;

/**
 * 事件消费上下文快照。
 * <p>
 * 发布事件时会将当前请求上下文中的关键字段冻结到该对象中，消费阶段再由
 * {@code ConsumeContextBinder} 恢复到线程上下文，从而保证日志追踪、用户身份、
 * 客户端信息等在异步链路中的可追溯性。
 * <p>
 * 首版快照采用固定字段白名单策略，不透传任意动态扩展属性，以控制序列化体积与演进风险。
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

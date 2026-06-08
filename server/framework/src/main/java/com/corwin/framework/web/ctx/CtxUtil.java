package com.corwin.framework.web.ctx;

import com.corwin.framework.constant.HttpHeaderNames;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.IpUtil;
import com.corwin.framework.web.auth.AuthPrincipal;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求上下文工具类。
 *
 * <p>
 * 该工具类基于 {@link ThreadLocal} 管理当前线程绑定的 {@link Ctx}，
 * 用于在一次请求处理过程中保存和访问请求级上下文数据。
 * </p>
 *
 * <h2>核心职责</h2>
 * <ul>
 *     <li>初始化请求上下文（trace、客户端、起始时间等）</li>
 *     <li>提供上下文数据的统一读写入口</li>
 *     <li>支持上下文快照与恢复，用于异步线程传播</li>
 *     <li>支持将上下文绑定到 MDC，便于日志链路追踪</li>
 * </ul>
 *
 * <h2>线程模型</h2>
 * <ul>
 *     <li>每个线程拥有独立的 {@link Ctx} 实例</li>
 *     <li>上下文默认不跨线程传播</li>
 *     <li>异步执行时应先调用 {@link #snapshot()}，再在目标线程调用 {@link #restore(Ctx)}</li>
 * </ul>
 *
 * <h2>生命周期建议</h2>
 * <ol>
 *     <li>请求进入时调用 {@link #initTrace(HttpServletRequest, String)}</li>
 *     <li>认证完成后调用 {@link #setPrincipal(AuthPrincipal)}</li>
 *     <li>日志输出前调用 {@link #bindMdc()}</li>
 *     <li>请求结束时必须调用 {@link #reset()}</li>
 * </ol>
 *
 * <h2>注意事项</h2>
 * <ul>
 *     <li>该类依赖 ThreadLocal，线程池复用场景下必须正确 reset，防止上下文污染</li>
 *     <li>不要将当前线程中的 Ctx 实例直接传给其他线程，应使用快照副本</li>
 *     <li>该类是静态工具类，不应被实例化</li>
 * </ul>
 *
 * @author Corwin
 * @since 2025/10/13
 */
public final class CtxUtil {

    /**
     * 当前线程绑定的请求上下文缓存。
     *
     * <p>
     * 采用 {@link ThreadLocal#withInitial(java.util.function.Supplier)} 方式懒初始化，
     * 保证调用方在首次访问时即可获得一个可用的 {@link Ctx} 实例。
     * </p>
     */
    private static final ThreadLocal<Ctx> CTX_CACHE = ThreadLocal.withInitial(Ctx::new);

    /**
     * 工具类不允许实例化
     */
    private CtxUtil() {
    }

    /**
     * 获取当前线程绑定的上下文对象。
     *
     * <p>
     * 若当前线程尚未初始化，则会自动创建一个空的 {@link Ctx} 实例。
     * </p>
     *
     * @return 当前线程上下文
     */
    public static Ctx get() {
        return CTX_CACHE.get();
    }

    /**
     * 获取当前上下文的快照副本。
     *
     * <p>
     * 主要用于异步任务、线程池、并发执行等场景下的上下文传播。
     * 返回的是副本而非原对象，避免多个线程共享同一个上下文实例。
     * </p>
     *
     * @return 当前上下文副本
     */
    public static Ctx snapshot() {
        return CTX_CACHE.get().copy();
    }

    /**
     * 将指定上下文恢复到当前线程。
     *
     * <p>
     * 常用于异步线程执行前恢复父线程的上下文信息。
     * 为避免调用方传入的实例在外部继续被修改，这里会保存其副本。
     * </p>
     *
     * <p>
     * 若传入 null，则会执行 {@link #reset()}，等价于清空当前线程上下文。
     * </p>
     *
     * @param ctx 要恢复的上下文
     */
    public static void restore(Ctx ctx) {
        if (ctx == null) {
            reset();
            return;
        }
        CTX_CACHE.set(ctx.copy());
    }

    /**
     * 初始化一次 HTTP 请求的基础上下文信息。
     *
     * <p>初始化内容包括：</p>
     * <ul>
     *     <li>请求开始时间（毫秒）</li>
     *     <li>请求开始时间（纳秒）</li>
     *     <li>traceId</li>
     *     <li>客户端标识（来自请求头）</li>
     *     <li>客户端 IP</li>
     * </ul>
     *
     * <p>
     * 通常在 Filter、Interceptor 或网关入口调用。
     * </p>
     *
     * @param request 当前 HTTP 请求
     * @param traceId 当前请求分配的 traceId
     */
    public static void initTrace(HttpServletRequest request, String traceId) {
        Ctx ctx = CTX_CACHE.get();
        ctx.setRequestStartTs(HighDate.mockTimestampMillis());
        ctx.setRequestStartNano(System.nanoTime());
        ctx.setTraceId(traceId);
        ctx.setClient(request.getHeader(HttpHeaderNames.CLIENT));
        ctx.setClientIp(IpUtil.getClientIp(request));
    }

    /**
     * 初始化调度任务场景下的上下文信息。
     *
     * <p>
     * 用于非 HTTP 请求线程，例如：
     * </p>
     * <ul>
     *     <li>定时任务</li>
     *     <li>后台任务</li>
     *     <li>消息消费任务</li>
     * </ul>
     *
     * <p>
     * 这类场景没有请求对象，因此 client 和 clientIp 置空。
     * </p>
     *
     * @param traceId 当前任务使用的 traceId
     */
    public static void schedulerInitTrace(String traceId) {
        Ctx ctx = CTX_CACHE.get();
        ctx.setRequestStartTs(HighDate.mockTimestampMillis());
        ctx.setRequestStartNano(System.nanoTime());
        ctx.setTraceId(traceId);
        ctx.setClient(null);
        ctx.setClientIp(null);
    }

    /**
     * 设置当前线程的 span 信息。
     *
     * @param spanId       当前 spanId
     * @param parentSpanId 父级 spanId
     */
    public static void setSpanInfo(String spanId, String parentSpanId) {
        Ctx ctx = CTX_CACHE.get();
        ctx.setSpanId(spanId);
        ctx.setParentSpanId(parentSpanId);
    }

    /**
     * 设置 traceId
     *
     * @param traceId 链路追踪 ID
     */
    public static void setTraceId(String traceId) {
        CTX_CACHE.get().setTraceId(traceId);
    }

    /**
     * 设置 spanId
     *
     * @param spanId 当前调用节点 ID
     */
    public static void setSpanId(String spanId) {
        CTX_CACHE.get().setSpanId(spanId);
    }

    /**
     * 设置当前认证主体。
     *
     * <p>
     * 一般在认证成功后调用。
     * </p>
     *
     * @param principal 当前用户身份
     */
    public static void setPrincipal(AuthPrincipal principal) {
        CTX_CACHE.get().setPrincipal(principal);
    }

    /**
     * 获取当前认证主体。
     *
     * @return 当前用户身份，若未认证则可能为 null
     */
    public static AuthPrincipal getPrincipal() {
        return CTX_CACHE.get().getPrincipal();
    }

    /**
     * 设置 token 哈希值。
     *
     * <p>
     * 用于记录当前认证 token 的摘要信息，便于审计、追踪或二次校验。
     * 不应保存明文 token。
     * </p>
     *
     * @param tokenHash token 摘要
     */
    public static void setTokenHash(String tokenHash) {
        CTX_CACHE.get().setTokenHash(tokenHash);
    }

    /**
     * 获取当前 token 哈希值。
     *
     * @return token 摘要
     */
    public static String getTokenHash() {
        return CTX_CACHE.get().getTokenHash();
    }

    /**
     * 清除当前 token 哈希值。
     */
    public static void clearTokenHash() {
        CTX_CACHE.get().setTokenHash(null);
    }

    /**
     * 写入扩展属性。
     *
     * <p>
     * 用于保存当前请求过程中的附加上下文数据。
     * 例如：业务标识、扩展安全信息、灰度信息等。
     * </p>
     *
     * @param key   属性键
     * @param value 属性值
     */
    public static void putAttr(String key, Object value) {
        CTX_CACHE.get().putAttr(key, value);
    }

    /**
     * 删除扩展属性。
     *
     * @param key 属性键
     */
    public static void removeAttr(String key) {
        CTX_CACHE.get().removeAttr(key);
    }

    /**
     * 按指定类型获取扩展属性。
     *
     * <p>
     * 若属性不存在，返回 null；
     * 若属性存在但类型不匹配，将抛出 {@link ClassCastException}。
     * </p>
     *
     * @param key  属性键
     * @param type 期望类型
     * @param <T>  类型参数
     * @return 属性值
     */
    public static <T> T getAttr(String key, Class<T> type) {
        return CTX_CACHE.get().getAttr(key, type);
    }

    /**
     * 将当前上下文绑定到 MDC。
     *
     * <p>
     * 常用于请求入口、异步线程恢复后，使日志自动带上 traceId、userId、clientIp 等字段。
     * </p>
     */
    public static void bindMdc() {
        CTX_CACHE.get().bindToMdc();
    }

    /**
     * 清理 MDC 中的上下文字段。
     *
     * <p>
     * 通常在请求结束时调用，防止线程复用带来的日志污染。
     * </p>
     */
    public static void clearMdc() {
        Ctx.clearMdc();
    }

    /**
     * 重置当前线程上下文。
     *
     * <p>该操作会：</p>
     * <ul>
     *     <li>清理 MDC</li>
     *     <li>移除当前线程的 ThreadLocal 上下文</li>
     * </ul>
     *
     * <p>
     * 这是请求结束时必须执行的清理动作。
     * 否则在线程池复用场景中，可能导致上一个请求的数据泄露到下一个请求。
     * </p>
     */
    public static void reset() {
        clearMdc();
        CTX_CACHE.remove();
    }

    /**
     * 获取请求开始时间（毫秒时间戳）
     *
     * @return 请求起始时间
     */
    public static long getRequestStartTs() {
        return CTX_CACHE.get().getRequestStartTs();
    }

    /**
     * 获取请求开始时间（纳秒）
     *
     * @return 请求起始纳秒时间
     */
    public static long getRequestStartNano() {
        return CTX_CACHE.get().getRequestStartNano();
    }

    /**
     * 获取当前 traceId
     *
     * @return traceId
     */
    public static String getTraceId() {
        return CTX_CACHE.get().getTraceId();
    }

    /**
     * 获取当前 traceId，若不存在则抛出异常。
     *
     * <p>
     * 适用于必须要求当前线程已完成 trace 初始化的场景。
     * </p>
     *
     * @return 非空 traceId
     * @throws IllegalStateException 当前线程未设置 traceId 时抛出
     */
    public static String getRequiredTraceId() {
        String traceId = getTraceId();
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalStateException("Current thread traceId is missing");
        }
        return traceId;
    }

    /**
     * 获取当前 spanId
     *
     * @return spanId
     */
    public static String getSpanId() {
        return CTX_CACHE.get().getSpanId();
    }

    /**
     * 获取父级 spanId
     *
     * @return parentSpanId
     */
    public static String getParentSpanId() {
        return CTX_CACHE.get().getParentSpanId();
    }

    /**
     * 获取客户端标识
     *
     * @return client 标识
     */
    public static String getClient() {
        return CTX_CACHE.get().getClient();
    }

    /**
     * 获取客户端 IP
     *
     * @return 客户端 IP
     */
    public static String getClientIp() {
        return CTX_CACHE.get().getClientIp();
    }
}

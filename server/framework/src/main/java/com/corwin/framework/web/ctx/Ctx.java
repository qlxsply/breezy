package com.corwin.framework.web.ctx;

import com.corwin.framework.constant.MdcKeys;
import com.corwin.framework.web.auth.AuthPrincipal;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求上下文对象（Request Context）。
 *
 * <p>
 * 该类用于在一次 HTTP 请求生命周期内，集中存储与请求相关的上下文信息，
 * 并通过 ThreadLocal（通常由 CtxUtil 持有）在不同层之间传递。
 * </p>
 *
 * <h2>设计目标</h2>
 * <ul>
 *     <li>统一承载请求级数据（Tracing / 用户 / 客户端 / 自定义扩展）</li>
 *     <li>避免在方法间层层传参</li>
 *     <li>支持日志追踪（通过 MDC 注入）</li>
 *     <li>支持跨线程传播（通过 copy 方法）</li>
 * </ul>
 *
 * <h2>线程模型</h2>
 * <ul>
 *     <li>通常绑定在 ThreadLocal 中（每个请求线程独立）</li>
 *     <li>非线程安全，不应在多个线程共享同一个实例</li>
 *     <li>跨线程使用时必须调用 {@link #copy()}</li>
 * </ul>
 *
 * <h2>典型使用场景</h2>
 * <ul>
 *     <li>网关 / Filter 初始化上下文</li>
 *     <li>认证模块写入用户信息</li>
 *     <li>日志系统读取 traceId / userId</li>
 *     <li>业务代码存放临时上下文数据</li>
 * </ul>
 *
 * @author Corwin
 * @since 2026/3/30
 */
@Getter
@Setter
public class Ctx {

    /**
     * 请求开始时间（毫秒时间戳）
     * 用于日志统计、请求耗时计算
     */
    private long requestStartTs;

    /**
     * 请求开始时间（纳秒级）
     * 用于更高精度的性能分析
     */
    private long requestStartNano;

    /**
     * 分布式追踪 ID（TraceId）
     * 用于串联整个调用链
     */
    private String traceId;

    /**
     * 当前 Span ID
     */
    private String spanId;

    /**
     * 父级 Span ID
     */
    private String parentSpanId;

    /**
     * 客户端标识（如 web / app / service）
     */
    private String client;

    /**
     * 客户端 IP 地址
     */
    private String clientIp;

    /**
     * 当前请求的认证主体（用户信息）
     */
    private AuthPrincipal principal;

    /**
     * Token 的哈希值（用于安全校验或日志追踪）
     * 注意：不应存储明文 token
     */
    private String tokenHash;

    /**
     * 扩展属性容器（用于存储自定义上下文数据）
     *
     * <p>特点：</p>
     * <ul>
     *     <li>懒初始化（避免无用内存开销）</li>
     *     <li>key-value 结构</li>
     *     <li>无类型约束（调用方负责类型安全）</li>
     * </ul>
     */
    private Map<String, Object> attributes;

    /**
     * 创建当前上下文的副本（浅拷贝）。
     *
     * <p>
     * 用于跨线程传播上下文，例如：
     * </p>
     * <ul>
     *     <li>线程池任务</li>
     *     <li>异步执行</li>
     *     <li>CompletableFuture</li>
     * </ul>
     *
     * <p>注意：</p>
     * <ul>
     *     <li>attributes 为浅拷贝（Map 复制，但 value 仍为引用）</li>
     *     <li>AuthPrincipal 为不可变对象（record），可安全共享</li>
     * </ul>
     *
     * @return 新的 Ctx 实例
     */
    public Ctx copy() {
        Ctx copy = new Ctx();
        copy.requestStartTs = this.requestStartTs;
        copy.requestStartNano = this.requestStartNano;
        copy.traceId = this.traceId;
        copy.spanId = this.spanId;
        copy.parentSpanId = this.parentSpanId;
        copy.client = this.client;
        copy.clientIp = this.clientIp;
        copy.principal = this.principal;
        copy.tokenHash = this.tokenHash;
        if (this.attributes != null) {
            copy.attributes = new HashMap<>(this.attributes);
        }
        return copy;
    }

    /**
     * 将上下文信息绑定到 SLF4J MDC（Mapped Diagnostic Context）。
     *
     * <p>
     * MDC 常用于日志系统，使日志自动携带上下文信息，例如：
     * traceId、userId、clientIp 等。
     * </p>
     *
     * <p>调用时机：</p>
     * <ul>
     *     <li>请求入口（Filter / Interceptor）</li>
     *     <li>线程切换后重新绑定</li>
     * </ul>
     *
     * <p>注意：</p>
     * <ul>
     *     <li>MDC 基于 ThreadLocal，线程切换后需要重新绑定</li>
     *     <li>必须在请求结束时调用 {@link #clearMdc()}</li>
     * </ul>
     */
    public void bindToMdc() {
        if (traceId != null) {
            MDC.put(MdcKeys.TRACE_ID, traceId);
        }
        if (spanId != null) {
            MDC.put(MdcKeys.SPAN_ID, spanId);
        }
        if (principal != null && principal.userId() != null) {
            MDC.put(MdcKeys.USER_ID, String.valueOf(principal.userId()));
        }
        if (principal != null && principal.username() != null) {
            MDC.put(MdcKeys.USER_NAME, principal.username());
        }
        if (clientIp != null) {
            MDC.put(MdcKeys.CLIENT_IP, clientIp);
        }
    }

    /**
     * 清理 MDC 中的上下文数据。
     *
     * <p>必须在请求结束时调用，防止 ThreadLocal 污染：</p>
     * <ul>
     *     <li>线程复用导致日志串数据</li>
     *     <li>用户信息泄露</li>
     * </ul>
     */
    public static void clearMdc() {
        MDC.remove(MdcKeys.TRACE_ID);
        MDC.remove(MdcKeys.SPAN_ID);
        MDC.remove(MdcKeys.USER_ID);
        MDC.remove(MdcKeys.USER_NAME);
        MDC.remove(MdcKeys.CLIENT_IP);
    }

    /**
     * 写入扩展属性
     *
     * @param key   属性名
     * @param value 属性值
     */
    public void putAttr(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }

    /**
     * 删除扩展属性
     *
     * <p>当 attributes 为空时会自动置为 null，以减少内存占用</p>
     */
    public void removeAttr(String key) {
        if (attributes == null) {
            return;
        }
        attributes.remove(key);
        if (attributes.isEmpty()) {
            attributes = null;
        }
    }

    /**
     * 获取扩展属性（无类型校验）
     */
    public Object getAttr(String key) {
        return attributes == null ? null : attributes.get(key);
    }

    /**
     * 获取扩展属性（带类型校验）
     *
     * @param key  属性名
     * @param type 期望类型
     * @return 属性值
     * @throws ClassCastException 类型不匹配时抛出
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttr(String key, Class<T> type) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            String name = type.getName();
            String vname = value.getClass().getName();
            String msg = String.format("Attribute [%s] is not of type %s, actual type: %s", key, name, vname);
            throw new ClassCastException(msg);
        }
        return (T) value;
    }
}

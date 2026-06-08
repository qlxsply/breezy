package com.corwin.framework.web.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SSE（Server-Sent Events）专用短时票据服务。
 *
 * <p>设计背景：</p>
 * <ul>
 *     <li>SSE 客户端通常使用浏览器原生 {@code EventSource} 建立连接。</li>
 *     <li>{@code EventSource} 对自定义请求头支持有限，实际场景中通常不方便携带
 *     {@code Authorization: Bearer xxx} 这类标准鉴权头。</li>
 *     <li>因此需要一种“只用于 SSE 建连阶段”的临时凭证机制，将常规登录态转换为
 *     可通过 URL 参数传递的短时票据。</li>
 * </ul>
 *
 * <p>典型流程：</p>
 * <ol>
 *     <li>前端先通过普通受保护接口（携带 JWT / Token）调用签发接口。</li>
 *     <li>服务端根据当前登录用户信息签发一个短时、一次性的 SSE ticket。</li>
 *     <li>前端使用该 ticket 作为查询参数访问 SSE stream 接口。</li>
 *     <li>服务端在鉴权过滤器中消费 ticket，并还原出用户身份上下文。</li>
 * </ol>
 *
 * <p>设计目标：</p>
 * <ul>
 *     <li>仅用于 SSE 建连，不替代常规 Token。</li>
 *     <li>ticket 应当是短时有效的。</li>
 *     <li>ticket 应当支持一次性消费，防止重放。</li>
 *     <li>ticket 认证成功后可恢复出 {@link TokenPayload}，供后续请求上下文使用。</li>
 * </ul>
 *
 * <p>安全说明：</p>
 * <ul>
 *     <li>本服务生成的 ticket 不应作为长期身份凭证使用。</li>
 *     <li>ticket 更适合通过 HTTPS 下的查询参数短暂传递。</li>
 *     <li>由于查询参数可能出现在访问日志中，建议控制 ticket 的 TTL 并避免日志明文记录。</li>
 * </ul>
 *
 * <p>实现说明：</p>
 * <ul>
 *     <li>当前接口不限定具体存储方式，可由内存、Redis 或数据库实现。</li>
 *     <li>在单机场景下可采用内存实现；分布式场景下通常应改为共享存储。</li>
 * </ul>
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/19
 */
public interface SseTicketService {

    /**
     * 签发一个新的 SSE 短时票据。
     *
     * <p>该方法通常在用户已通过常规 Token 鉴权后调用，将当前用户身份信息
     * 转换为一个“仅用于 SSE 建连阶段”的短时票据。</p>
     *
     * <p>返回的 ticket 应满足以下约束：</p>
     * <ul>
     *     <li>足够随机，难以猜测；</li>
     *     <li>存在明确过期时间；</li>
     *     <li>可在后续 {@link #consume(String)} 时还原出对应用户身份。</li>
     * </ul>
     *
     * @param payload 当前用户身份载荷，通常来自已验证通过的登录态
     * @return 新签发的票据对象，包含票据值及过期时间
     */
    SseTicket issue(TokenPayload payload);

    /**
     * 消费一个 SSE 短时票据，并还原出原始身份载荷。
     *
     * <p>“消费”强调该 ticket 通常是一次性的：一旦成功读取，就不应再次复用。</p>
     *
     * <p>典型校验项包括：</p>
     * <ul>
     *     <li>ticket 非空；</li>
     *     <li>ticket 存在；</li>
     *     <li>ticket 未过期；</li>
     *     <li>ticket 未被重复使用。</li>
     * </ul>
     *
     * <p>该方法通常在鉴权过滤器中调用，仅对 SSE stream 请求生效。</p>
     *
     * @param ticket 前端提交的票据值
     * @return 与该票据绑定的用户身份载荷
     * @throws IllegalArgumentException 当 ticket 为空、无效、已过期或已被消费时抛出
     */
    TokenPayload consume(String ticket);

    /**
     * 判断当前请求是否应使用 SSE ticket 机制进行鉴权。
     *
     * <p>该方法的用途是将 ticket 认证能力限制在特定请求范围内，
     * 避免普通接口误用 SSE ticket。</p>
     *
     * <p>通常会根据以下条件判断：</p>
     * <ul>
     *     <li>HTTP 方法是否符合要求（一般为 GET）；</li>
     *     <li>请求路径是否为 SSE stream 接口；</li>
     *     <li>必要时还可结合 Content-Type、Accept 等信息进一步收敛。</li>
     * </ul>
     *
     * @param request 当前 HTTP 请求
     * @return true 表示当前请求支持使用 SSE ticket 鉴权；false 表示不支持
     */
    boolean supports(HttpServletRequest request);

    /**
     * SSE 短时票据对象。
     *
     * @param value                票据值，通常为高随机、URL 安全的短字符串
     * @param expiresAtEpochMillis 票据过期时间戳（毫秒）
     */
    record SseTicket(
            String value,
            long expiresAtEpochMillis
    ) {
    }
}
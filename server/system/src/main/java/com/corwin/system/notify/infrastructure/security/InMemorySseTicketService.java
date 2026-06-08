package com.corwin.system.notify.infrastructure.security;

import com.corwin.system.config.application.config.SystemConfigKeys;
import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.auth.SseTicketService;
import com.corwin.framework.web.auth.TokenPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link SseTicketService} 的单机内存实现。
 *
 * <p>该实现适用于单节点部署场景，通过内存 {@link ConcurrentHashMap} 保存
 * ticket 与身份载荷的映射关系。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *     <li>ticket 使用 {@link SecureRandom} 生成，具备较高随机性；</li>
 *     <li>ticket 为短时有效；</li>
 *     <li>ticket 为一次性消费，成功读取后立即删除；</li>
 *     <li>定时清理过期 ticket，避免内存积累。</li>
 * </ul>
 *
 * <p>适用范围：</p>
 * <ul>
 *     <li>单机部署；</li>
 *     <li>SSE 建连阶段的临时认证；</li>
 *     <li>不要求 ticket 在多实例间共享。</li>
 * </ul>
 *
 * <p>不适用场景：</p>
 * <ul>
 *     <li>多节点部署；</li>
 *     <li>服务重启后需保留 ticket 状态；</li>
 *     <li>需要高可靠、持久化票据存储的场景。</li>
 * </ul>
 *
 * @author Corwin 2026/3/30
 * @since 2026/3/19
 */
@Service
public class InMemorySseTicketService implements SseTicketService {

    /**
     * 用于生成高随机 ticket 的安全随机数生成器。
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * ticket 最小有效期（秒）。
     *
     * <p>即使外部配置过小，也会被该下限保护，避免 ticket 刚签发即过期。</p>
     */
    private static final int MIN_TTL_SECONDS = 10;

    /**
     * SSE 流接口路径。
     *
     * <p>仅该路径对应的请求允许使用 SSE ticket 机制进行认证。</p>
     */
    private static final String SSE_STREAM_PATH = "/api/sse/stream";

    /**
     * ticket 存储。
     *
     * <p>key 为 ticket 值，value 为票据条目（身份载荷 + 过期时间）。</p>
     */
    private final Map<String, TicketEntry> store = new ConcurrentHashMap<>();

    /**
     * 签发新的 SSE ticket。
     *
     * <p>签发逻辑：</p>
     * <ol>
     *     <li>读取当前时间；</li>
     *     <li>根据配置计算 TTL，并施加最小值保护；</li>
     *     <li>生成随机 ticket 值；</li>
     *     <li>将 ticket 与身份载荷、过期时间写入内存；</li>
     *     <li>返回票据对象给调用方。</li>
     * </ol>
     *
     * @param payload 用户身份载荷
     * @return SSE ticket
     */
    @Override
    public SseTicket issue(TokenPayload payload) {
        Instant now = HighDate.mockInstant();
        long ttlSeconds = Math.max(MIN_TTL_SECONDS, ConfigRegistry.longV(SystemConfigKeys.SSE_TICKET_TTL_SECONDS));
        Instant expiresAt = now.plusSeconds(ttlSeconds);
        String ticketValue = generateTicket();

        store.put(ticketValue, new TicketEntry(payload, expiresAt));
        return new SseTicket(ticketValue, expiresAt.toEpochMilli());
    }

    /**
     * 消费一个 ticket，并返回其绑定的用户身份载荷。
     *
     * <p>本实现采用“一次性消费”语义：调用时会先从存储中移除 ticket，
     * 再校验其是否存在以及是否过期。</p>
     *
     * <p>这样做的主要目的是：</p>
     * <ul>
     *     <li>防止同一个 ticket 被重复使用；</li>
     *     <li>降低 ticket 被窃取后重放的风险。</li>
     * </ul>
     *
     * @param ticket ticket 值
     * @return 原始身份载荷
     * @throws IllegalArgumentException 当 ticket 为空、找不到或已过期时抛出
     */
    @Override
    public TokenPayload consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new IllegalArgumentException("sse ticket is blank");
        }
        TicketEntry entry = store.remove(ticket.trim());
        if (entry == null) {
            throw new IllegalArgumentException("sse ticket not found");
        }
        if (entry.expiresAt().isBefore(HighDate.mockInstant())) {
            throw new IllegalArgumentException("sse ticket expired");
        }
        return entry.payload();
    }

    /**
     * 判断当前请求是否支持使用 SSE ticket 认证。
     *
     * <p>当前规则：</p>
     * <ul>
     *     <li>必须为 GET 请求；</li>
     *     <li>请求路径必须命中 SSE stream 路径。</li>
     * </ul>
     *
     * @param request 当前请求
     * @return true 表示支持；false 表示不支持
     */
    @Override
    public boolean supports(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri != null && uri.endsWith(SSE_STREAM_PATH);
    }

    /**
     * 定时清理已过期但尚未被消费的 ticket。
     *
     * <p>该任务仅用于回收内存，不影响 ticket 的鉴权正确性；
     * 即使尚未来得及清理，{@link #consume(String)} 也会在读取时再次做过期校验。</p>
     */
    @Scheduled(fixedDelay = 60_000L)
    public void cleanupExpired() {
        Instant now = HighDate.mockInstant();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    /**
     * 生成高随机、URL 安全的 ticket 值。
     *
     * <p>实现方式：</p>
     * <ul>
     *     <li>随机生成 24 字节数据；</li>
     *     <li>使用 Base64 URL Safe 编码；</li>
     *     <li>移除 padding，减少长度并便于放入 URL。</li>
     * </ul>
     *
     * @return ticket 字符串
     */
    private String generateTicket() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * ticket 存储条目。
     *
     * @param payload   票据绑定的身份载荷
     * @param expiresAt 票据过期时间
     */
    private record TicketEntry(
            TokenPayload payload,
            Instant expiresAt
    ) {
    }
}

package com.corwin.system.notify.infrastructure.security;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.web.auth.SseTicketService;
import com.corwin.framework.web.auth.TokenPayload;
import com.corwin.system.notify.config.SystemNotifyConfigSpecs;
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
 * @author Corwin 2026/3/30
 */
@Service
public class InMemorySseTicketService implements SseTicketService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SSE_STREAM_PATH = "/api/sse/stream";

    private final Map<String, TicketEntry> store = new ConcurrentHashMap<>();

    @Override
    public SseTicket issue(TokenPayload payload) {
        Instant now = HighDate.mockInstant();
        long ttlSeconds = Configs.get(SystemNotifyConfigSpecs.SSE).ttlSeconds();
        Instant expiresAt = now.plusSeconds(ttlSeconds);
        String ticketValue = generateTicket();
        store.put(ticketValue, new TicketEntry(payload, expiresAt));
        return new SseTicket(ticketValue, expiresAt.toEpochMilli());
    }

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

    @Override
    public boolean supports(HttpServletRequest request) {
        return HttpMethod.GET.matches(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().endsWith(SSE_STREAM_PATH);
    }

    @Scheduled(fixedDelay = 60_000L)
    public void cleanupExpired() {
        Instant now = HighDate.mockInstant();
        store.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private String generateTicket() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record TicketEntry(TokenPayload payload, Instant expiresAt) {
    }
}

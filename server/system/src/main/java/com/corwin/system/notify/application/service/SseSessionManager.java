package com.corwin.system.notify.application.service;

import com.corwin.framework.constant.UserType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 会话管理器
 * 支持单例管理、心跳、以及基于内存缓冲区的 Last-Event-ID 补发。
 *
 * @author Corwin 2026/3/16
 */
@Slf4j
@Component
public class SseSessionManager {

    private final Map<UserSessionKey, UserSession> sessions = new ConcurrentHashMap<>();

    /**
     * 注册一个新的 SSE 会话。
     */
    public void register(Long userId, UserType userType, SseEmitter emitter) {
        UserSessionKey sessionKey = new UserSessionKey(userId, userType);
        UserSession current = new UserSession(UUID.randomUUID().toString(), emitter);
        UserSession old = sessions.put(sessionKey, current);
        if (old != null) {
            try {
                old.emitter().complete();
            } catch (Exception ignored) {
            }
        }

        emitter.onCompletion(() -> removeIfSame(sessionKey, current));
        emitter.onTimeout(() -> removeIfSame(sessionKey, current));
        emitter.onError((e) -> {
            log.debug("SSE emitter error for userType={}, userId={}: {}", userType, userId, e.getMessage());
            removeIfSame(sessionKey, current);
        });
    }

    /**
     * 向指定用户发送消息。
     * 消息会自动加入内存缓冲区以支持断线补发。
     */
    public boolean send(Long userId, UserType userType, String eventName, Object data) {
        String id = UUID.randomUUID().toString();
        return send(userId, userType, eventName, data, id);
    }

    public boolean send(Long userId, UserType userType, String eventName, Object data, String eventId) {
        String id = eventId;
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        SseMessage msg = new SseMessage(id, eventName, data);

        // 推送给当前在线会话
        UserSessionKey sessionKey = new UserSessionKey(userId, userType);
        UserSession session = sessions.get(sessionKey);
        if (session != null) {
            try {
                sendToEmitter(session.emitter(), msg);
                return true;
            } catch (IOException e) {
                log.error("SSE push failed for userType={}, userId={}", userType, userId, e);
                removeIfSame(sessionKey, session);
            }
        }
        return false;
    }

    private void sendToEmitter(SseEmitter emitter, SseMessage msg) throws IOException {
        emitter.send(SseEmitter.event().id(msg.id()).name(msg.name()).data(msg.data()));
    }

    public void remove(Long userId, UserType userType) {
        sessions.remove(new UserSessionKey(userId, userType));
    }

    @Scheduled(fixedDelay = 30_000L)
    public void heartbeat() {
        for (Map.Entry<UserSessionKey, UserSession> entry : sessions.entrySet()) {
            try {
                entry.getValue().emitter().send(SseEmitter.event().name("HEARTBEAT").data("ping"));
            } catch (IOException e) {
                removeIfSame(entry.getKey(), entry.getValue());
            }
        }
    }

    private void removeIfSame(UserSessionKey sessionKey, UserSession candidate) {
        sessions.computeIfPresent(sessionKey, (key, current) -> {
            if (current.sessionId().equals(candidate.sessionId())) {
                return null;
            }
            return current;
        });
    }

    private record UserSessionKey(
            Long userId,
            UserType userType
    ) {
    }

    private record UserSession(
            String sessionId,
            SseEmitter emitter
    ) {
    }

    public record SseMessage(
            String id,
            String name,
            Object data
    ) {
    }
}

package com.corwin.system.notify.config;

import com.corwin.framework.config.definition.*;
import com.corwin.system.notify.domain.model.MsgPriority;
import com.corwin.system.notify.published.MsgType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

import static com.corwin.framework.config.definition.ConfigFieldSpecs.optional;
import static com.corwin.framework.config.definition.ConfigFieldSpecs.optionalSensitive;

/**
 * @author Corwin 2026/7/31
 */
public final class SystemNotifyConfigSpecs {

    public static final ConfigSpec<SseTicketConfig> SSE = new SimpleConfigSpec<>(new ConfigKey("system.notify.sse"),
            "system", "notify", "SSE Ticket", "SSE 一次性 ticket 有效时间", SseTicketConfig.class,
            new SseTicketConfig(60), 1, 10, ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE,
            ConfigInvalidValuePolicy.USE_DEFAULT, List.of(optional("ttlSeconds", "有效秒数", ConfigFieldType.LONG, 10)),
            "default", SystemNotifyConfigSpecs::validateSse, SystemNotifyConfigSpecs::validateSse);

    public static final ConfigSpec<WebPushVapidConfig> WEB_PUSH = new SimpleConfigSpec<>(
            new ConfigKey("system.notify.web-push"), "system", "notify", "Web Push", "Web Push VAPID 公私钥和联系主体",
            WebPushVapidConfig.class, new WebPushVapidConfig("", "", "mailto:breezy@localhost"), 1, 20,
            ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE, ConfigInvalidValuePolicy.FAIL_STARTUP,
            List.of(optional("publicKey", "VAPID 公钥", ConfigFieldType.STRING, 10),
                    optionalSensitive("privateKey", "VAPID 私钥", ConfigFieldType.STRING, 20),
                    optional("subject", "VAPID 联系主体", ConfigFieldType.STRING, 30)), "web-push-vapid",
            SystemNotifyConfigSpecs::validateWebPush, SystemNotifyConfigSpecs::validateWebPush);

    public static final ConfigSpec<MessageTypeConfigs> MESSAGE_TYPES = new SimpleConfigSpec<>(
            new ConfigKey("system.notify.message-types"), "system", "notify", "消息类型策略",
            "不同消息类型的通道、路由和优先级策略", MessageTypeConfigs.class, new MessageTypeConfigs(
            List.of(new MessageTypeConfig(MsgType.TODO_REMINDER, "/todo/all", MsgPriority.MEDIUM, true, false, true,
                            false), new MessageTypeConfig(MsgType.SYSTEM_EVENT, "", MsgPriority.LOW, true, false, false, false),
                    new MessageTypeConfig(MsgType.BUSINESS_EVENT, "", MsgPriority.MEDIUM, true, false, true, false))),
            1, 30, ConfigActivationPolicy.DYNAMIC, ConfigEditPolicy.ADMIN_EDITABLE,
            ConfigInvalidValuePolicy.USE_DEFAULT,
            List.of(optional("items", "消息类型配置", ConfigFieldType.OBJECT, 10)), "message-type-config",
            SystemNotifyConfigSpecs::validateMessageTypes, SystemNotifyConfigSpecs::validateMessageTypes);

    public record SseTicketConfig(long ttlSeconds) {
    }

    public record WebPushVapidConfig(
            String publicKey,
            String privateKey,
            String subject
    ) {
    }

    public record MessageTypeConfigs(List<MessageTypeConfig> items) {
        public MessageTypeConfigs {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record MessageTypeConfig(
            MsgType msgType,
            String route,
            MsgPriority priority,
            boolean sseEnabled,
            boolean webPushEnabled,
            boolean panelAutoOpen,
            boolean osNotificationEnabled
    ) {
    }

    private static List<ConfigViolation> validateSse(SseTicketConfig value) {
        return value.ttlSeconds() >= 10 ? List.of() : List.of(
                new ConfigViolation("ttlSeconds", "OUT_OF_RANGE", "SSE ticket 有效时间不能少于 10 秒"));
    }

    private static List<ConfigViolation> validateWebPush(WebPushVapidConfig value) {
        var violations = new ArrayList<ConfigViolation>();
        boolean hasPublic = value.publicKey() != null && !value.publicKey().isBlank();
        boolean hasPrivate = value.privateKey() != null && !value.privateKey().isBlank();
        if (hasPublic != hasPrivate) {
            violations.add(new ConfigViolation("publicKey", "KEY_PAIR_REQUIRED", "VAPID 公钥和私钥必须同时配置"));
        }
        if (hasPublic && (!validBase64(value.publicKey(), 65) || !validBase64(value.privateKey(), 32))) {
            violations.add(new ConfigViolation("publicKey", "INVALID_KEY", "VAPID 密钥格式无效"));
        }
        if (value.subject() == null || value.subject().isBlank() || !validSubject(value.subject())) {
            violations.add(new ConfigViolation("subject", "INVALID_URI", "VAPID 联系主体必须是 mailto 或 https URI"));
        }
        return violations;
    }

    private static List<ConfigViolation> validateMessageTypes(MessageTypeConfigs value) {
        var violations = new ArrayList<ConfigViolation>();
        var types = new HashSet<MsgType>();
        for (int index = 0; index < value.items().size(); index++) {
            MessageTypeConfig item = value.items().get(index);
            if (item == null || item.msgType() == null || item.priority() == null || !types.add(item.msgType())) {
                violations.add(new ConfigViolation("items[" + index + "]", "INVALID_ITEM", "消息类型配置无效或重复"));
                continue;
            }
            if (item.route() != null && !item.route().isBlank() && !item.route().startsWith("/")) {
                violations.add(
                        new ConfigViolation("items[" + index + "].route", "INVALID_ROUTE", "消息路由必须以 / 开头"));
            }
        }
        return violations;
    }

    private static boolean validBase64(String value, int expectedLength) {
        try {
            return Base64.getUrlDecoder().decode(value).length == expectedLength;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean validSubject(String value) {
        try {
            String scheme = URI.create(value).getScheme();
            return "mailto".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private SystemNotifyConfigSpecs() {
    }
}

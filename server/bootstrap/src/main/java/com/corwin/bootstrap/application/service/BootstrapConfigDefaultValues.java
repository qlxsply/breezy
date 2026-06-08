package com.corwin.bootstrap.application.service;

import com.corwin.config.BusinessConfigKeys;
import com.corwin.framework.config.DefaultConfigKeys;
import com.corwin.framework.config.UserDateFormatOption;
import com.corwin.framework.config.UserDateTimeFormatOption;
import com.corwin.framework.config.UserDecimalFormatOption;
import com.corwin.framework.config.UserTimeZoneOption;
import com.corwin.framework.util.ClientIpMode;
import com.corwin.system.config.application.config.SystemConfigKeys;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Corwin 2026/5/5
 */
public final class BootstrapConfigDefaultValues {

    public static Map<String, String> loadAll() {
        Map<String, String> values = new LinkedHashMap<>();

        values.put(DefaultConfigKeys.TIME_OFFSET.name(), "0");
        values.put(DefaultConfigKeys.CLIENT_IP_MODE.name(), ClientIpMode.REMOTE_ADDR.name());
        values.put(DefaultConfigKeys.AUTH_WHITELIST.name(), defaultAuthWhitelist());
        values.put(DefaultConfigKeys.LOGGING_FILTER_EXCLUDE_PREFIXES.name(), defaultLoggingFilterExcludePrefixes());
        values.put(DefaultConfigKeys.LOGGING_FILTER_STREAM_PREFIXES.name(), defaultLoggingFilterStreamPrefixes());
        values.put(DefaultConfigKeys.USER_TIME_ZONE.name(), UserTimeZoneOption.ASIA_SHANGHAI.name());
        values.put(DefaultConfigKeys.USER_DATE_TIME_FORMAT.name(),
                UserDateTimeFormatOption.YYYY_MM_DD_HH_MM_SS.name());
        values.put(DefaultConfigKeys.USER_DATE_FORMAT.name(), UserDateFormatOption.YYYY_MM_DD.name());
        values.put(DefaultConfigKeys.USER_DECIMAL_FORMAT.name(), UserDecimalFormatOption.COMMA_2.name());

        values.put(SystemConfigKeys.ACCESS_TOKEN_TTL_SECONDS.name(), String.valueOf(24 * 60 * 60));
        values.put(SystemConfigKeys.ACCESS_TOKEN_SECRET.name(), "k6V1rR6YpH3X8M9EwJ7cFQZs2pR5D4N8tLxUeA0mBvY=");
        values.put(SystemConfigKeys.PASSWORD_MIN_LENGTH.name(), "8");
        values.put(SystemConfigKeys.PASSWORD_REQUIRE_DIGIT.name(), "true");
        values.put(SystemConfigKeys.PASSWORD_REQUIRE_LETTER.name(), "true");
        values.put(SystemConfigKeys.PASSWORD_REQUIRE_UPPER.name(), "false");
        values.put(SystemConfigKeys.PASSWORD_REQUIRE_LOWER.name(), "false");
        values.put(SystemConfigKeys.PASSWORD_REQUIRE_SPECIAL.name(), "false");
        values.put(SystemConfigKeys.PASSWORD_FORCE_CHANGE_ON_FIRST_LOGIN.name(), "false");
        values.put(SystemConfigKeys.PASSWORD_FORCE_CHANGE_ON_RESET.name(), "true");
        values.put(SystemConfigKeys.AUDIT_ENABLED.name(), "true");
        values.put(SystemConfigKeys.AUDIT_ASYNC_ENABLED.name(), "true");
        values.put(SystemConfigKeys.AUDIT_RECORD_REQUEST_MAX_LENGTH.name(), "4096");
        values.put(SystemConfigKeys.AUDIT_RECORD_RESPONSE_MAX_LENGTH.name(), "4096");
        values.put(SystemConfigKeys.STORAGE_BASE_PATH.name(), "./data/storage");
        values.put(SystemConfigKeys.SYSTEM_FILE_PREVIEW_MAX_SIZE.name(), "5242880");
        values.put(SystemConfigKeys.SSE_TICKET_TTL_SECONDS.name(), "60");
        values.put(SystemConfigKeys.WEB_PUSH_VAPID_PUBLIC_KEY.name(),
                "BA526rA_aSJnXCeC0Z-3ps7U7YbkjIPRVZzpEuw0hOLp28r6CDt4FCjWISROSNEVRCKskzzCmT1TJcgKRt4_Ous=");
        values.put(SystemConfigKeys.WEB_PUSH_VAPID_PRIVATE_KEY.name(), "AKIC_0WEBCL0dXPWKWxUNUflkMXsqJqRyl6952J5iGG9");
        values.put(SystemConfigKeys.WEB_PUSH_VAPID_SUBJECT.name(), "mailto:breezy@localhost");
        values.put(SystemConfigKeys.MSG_TYPE_CONFIGS.name(), defaultMsgTypeConfigs());

        values.put(BusinessConfigKeys.JSONFMT_CONTENT_FILE_THRESHOLD.name(), "61440");
        values.put(BusinessConfigKeys.SCHEMAFORGE_DDL_FORMAT_ENABLED.name(), "true");
        values.put(BusinessConfigKeys.SCHEMAFORGE_DDL_QUALIFIER_MODE.name(), "ALWAYS_SOURCE");
        return values;
    }

    private static String defaultAuthWhitelist() {
        return """
                [
                  {"type":"EXACT","pattern":"/api/auth/login"},
                  {"type":"EXACT","pattern":"/api/auth/logout"},
                  {"type":"EXACT","pattern":"/api/auth/me"},
                  {"type":"EXACT","pattern":"/h2-console"},
                  {"type":"ANT","pattern":"/h2-console/**"}
                ]
                """;
    }

    private static String defaultLoggingFilterExcludePrefixes() {
        List<String> list = new ArrayList<>();
        list.add("/static/");
        list.add("/actuator");
        list.add("/favicon.ico");
        return toJsonList(list);
    }

    private static String defaultLoggingFilterStreamPrefixes() {
        List<String> list = new ArrayList<>();
        list.add("/api/sse/");
        return toJsonList(list);
    }

    private static String defaultMsgTypeConfigs() {
        return """
                [
                  {
                    "msgType": "TODO_REMINDER",
                    "route": "/todo/all",
                    "priority": "MEDIUM",
                    "sseEnabled": true,
                    "webPushEnabled": false,
                    "panelAutoOpen": true,
                    "osNotificationEnabled": false
                  },
                  {
                    "msgType": "SYSTEM_EVENT",
                    "route": "",
                    "priority": "LOW",
                    "sseEnabled": true,
                    "webPushEnabled": false,
                    "panelAutoOpen": false,
                    "osNotificationEnabled": false
                  },
                  {
                    "msgType": "BUSINESS_EVENT",
                    "route": "",
                    "priority": "MEDIUM",
                    "sseEnabled": true,
                    "webPushEnabled": false,
                    "panelAutoOpen": true,
                    "osNotificationEnabled": false
                  }
                ]
                """;
    }

    private static String toJsonList(List<String> list) {
        try {
            return new ObjectMapper().writer().writeValueAsString(list);
        } catch (Exception ex) {
            throw new IllegalStateException("Serialize bootstrap default config value failed", ex);
        }
    }

    private BootstrapConfigDefaultValues() {
    }
}

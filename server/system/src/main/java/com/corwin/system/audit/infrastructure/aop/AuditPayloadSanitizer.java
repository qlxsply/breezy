package com.corwin.system.audit.infrastructure.aop;

import com.corwin.framework.config.ConfigRegistry;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.config.application.config.SystemConfigKeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.*;

/**
 * @author Corwin 2026/4/19
 */
@Component
@RequiredArgsConstructor
public class AuditPayloadSanitizer {

    private static final String MASK = "******";
    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "pwd", "oldpassword", "newpassword",
            "confirmpassword", "token", "accesstoken", "refreshtoken", "authorization", "secret", "secretkey",
            "privatekey", "credential", "credentials");

    public String summarizeRequestParameters(Map<String, String[]> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return null;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String[] values = entry.getValue();
            if (values == null) {
                normalized.put(entry.getKey(), null);
                continue;
            }
            if (values.length == 1) {
                normalized.put(entry.getKey(), values[0]);
                continue;
            }
            normalized.put(entry.getKey(), List.of(values));
        }
        return summarize(normalized, ConfigRegistry.intV(SystemConfigKeys.AUDIT_RECORD_REQUEST_MAX_LENGTH));
    }

    public String summarizeRequestBody(Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return null;
        }
        List<Object> payloads = new ArrayList<>();
        for (Object argument : arguments) {
            if (argument == null || isIgnoredArgument(argument)) {
                continue;
            }
            payloads.add(argument);
        }
        if (payloads.isEmpty()) {
            return null;
        }
        Object payload = payloads.size() == 1 ? payloads.get(0) : payloads;
        return summarize(payload, ConfigRegistry.intV(SystemConfigKeys.AUDIT_RECORD_REQUEST_MAX_LENGTH));
    }

    public String summarizeResponseBody(Object response) {
        Object payload = response;
        if (response instanceof ApiResponse<?> apiResponse) {
            payload = apiResponse.getData();
        }
        return summarize(payload, ConfigRegistry.intV(SystemConfigKeys.AUDIT_RECORD_RESPONSE_MAX_LENGTH));
    }

    public String summarizeErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = StrUtil.trimToNull(throwable.getMessage());
        if (message == null) {
            message = throwable.getClass().getName();
        }
        return truncate(message, ConfigRegistry.intV(SystemConfigKeys.AUDIT_RECORD_RESPONSE_MAX_LENGTH));
    }

    private String summarize(Object payload, int maxLength) {
        if (payload == null) {
            return null;
        }
        try {
            Object sanitized = sanitizePayload(payload);
            String text;
            if (sanitized instanceof String str) {
                text = str;
            } else {
                text = Json.toStr(sanitized);
            }
            return truncate(StrUtil.trimToNull(text), maxLength);
        } catch (RuntimeException ex) {
            return truncate(StrUtil.trimToNull(String.valueOf(payload)), maxLength);
        }
    }

    private Object sanitizePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (isSimpleValue(payload)) {
            return payload;
        }
        JsonNode root = Json.mapper().valueToTree(payload);
        return sanitizeNode(root, null);
    }

    private JsonNode sanitizeNode(JsonNode node, String fieldName) {
        if (fieldName != null && isSensitiveField(fieldName)) {
            return Json.mapper().getNodeFactory().textNode(MASK);
        }
        if (node == null || node.isNull()) {
            return NullNode.getInstance();
        }
        if (node.isObject()) {
            ObjectNode sanitized = Json.mapper().createObjectNode();
            node.fields().forEachRemaining(
                    entry -> sanitized.set(entry.getKey(), sanitizeNode(entry.getValue(), entry.getKey())));
            return sanitized;
        }
        if (node.isArray()) {
            ArrayNode sanitized = Json.mapper().createArrayNode();
            for (JsonNode item : node) {
                sanitized.add(sanitizeNode(item, null));
            }
            return sanitized;
        }
        return node.deepCopy();
    }

    private boolean isSensitiveField(String fieldName) {
        String normalized = StrUtil.trimToNull(fieldName);
        if (normalized == null) {
            return false;
        }
        return SENSITIVE_FIELDS.contains(normalized.toLowerCase(Locale.ROOT));
    }

    private boolean isIgnoredArgument(Object argument) {
        return argument instanceof ServletRequest || argument instanceof ServletResponse || argument instanceof MultipartFile || argument instanceof BindingResult || argument instanceof Errors || argument instanceof Model || argument instanceof RedirectAttributes || argument instanceof Principal || argument instanceof InputStreamSource;
    }

    private boolean isSimpleValue(Object payload) {
        return payload instanceof CharSequence || payload instanceof Number || payload instanceof Boolean || payload instanceof Enum<?> || payload instanceof java.time.temporal.Temporal || payload instanceof java.util.Date || payload instanceof java.util.UUID;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}

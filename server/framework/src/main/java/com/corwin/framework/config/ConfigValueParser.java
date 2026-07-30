package com.corwin.framework.config;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.SysException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

/**
 * Utility for parsing raw string config values into typed values (int, long, boolean, decimal, string list/set).
 * <p>
 * All methods throw {@link com.corwin.framework.error.SysException} on parse failure.
 *
 * @author Corwin 2026/1/31
 */
public final class ConfigValueParser {

    public static int parseInt(String key, String value) {
        if (value == null) {
            throw new SysException("配置项值为空，无法解析为 INT: " + key, BaseError.SERVICE_ERROR);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new SysException("配置项解析失败(INT): key=" + key + ", value=" + value, BaseError.SERVICE_ERROR);
        }
    }

    public static long parseLong(String key, String value) {
        if (value == null) {
            throw new SysException("配置项值为空，无法解析为 LONG: " + key, BaseError.SERVICE_ERROR);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            throw new SysException("配置项解析失败(LONG): key=" + key + ", value=" + value, BaseError.SERVICE_ERROR);
        }
    }

    public static boolean parseBool(String key, String value) {
        if (value == null) {
            throw new SysException("配置项值为空，无法解析为 BOOL: " + key, BaseError.SERVICE_ERROR);
        }
        String v = value.trim();
        if ("true".equalsIgnoreCase(v) || "1".equals(v) || "Y".equalsIgnoreCase(v)) {
            return true;
        }
        if ("false".equalsIgnoreCase(v) || "0".equals(v) || "N".equalsIgnoreCase(v)) {
            return false;
        }
        throw new SysException("配置项解析失败(BOOL): key=" + key + ", value=" + value, BaseError.SERVICE_ERROR);
    }

    public static BigDecimal parseDec(String key, String value) {
        if (value == null) {
            throw new SysException("配置项值为空，无法解析为 DEC: " + key, BaseError.SERVICE_ERROR);
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            throw new SysException("配置项解析失败(DEC): key=" + key + ", value=" + value, BaseError.SERVICE_ERROR);
        }
    }

    public static List<String> parseStrList(String key, String value) {
        if (value == null) {
            throw new SysException("配置项值为空，无法解析为 LIST_STR: " + key, BaseError.SERVICE_ERROR);
        }
        String raw = value.trim();
        if (raw.isEmpty()) {
            return List.of();
        }

        ObjectMapper json = new ObjectMapper();
        List<String> parsed;
        try {
            parsed = json.readValue(raw, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new SysException(e, BaseError.SERVICE_ERROR);
        }

        if (parsed == null || parsed.isEmpty()) {
            return List.of();
        }

        List<String> cleaned = new ArrayList<>();
        for (String item : parsed) {
            if (item == null) {
                continue;
            }
            String v = item.trim();
            if (v.isEmpty()) {
                continue;
            }
            cleaned.add(v);
        }
        return cleaned.isEmpty() ? List.of() : Collections.unmodifiableList(cleaned);
    }

    public static Set<String> parseStrSet(String key, String value) {
        List<String> list = parseStrList(key, value);
        if (list.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(list));
    }

    private ConfigValueParser() {
    }
}


package com.corwin.framework.config;

import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.SysException;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory container for system configuration values.
 * <p>
 * Stores parsed config values of various types (int, long, boolean, decimal, string,
 * string list, string set, custom) in separate typed caches. Thread-safe.
 *
 * @author Corwin 2025/10/14
 */
@Slf4j
public class ConfigRegistry {
    // All config items by key
    private static final Map<String, ConfigItem> ALL = new ConcurrentHashMap<>();
    // Typed caches, one map per value type
    private static final Map<String, Integer> KV_INT = new ConcurrentHashMap<>();
    private static final Map<String, Long> KV_LONG = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> KV_BOOL = new ConcurrentHashMap<>();
    private static final Map<String, BigDecimal> KV_DECIMAL = new ConcurrentHashMap<>();
    private static final Map<String, String> KV_STR = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> KV_STR_LIST = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> KV_STR_SET = new ConcurrentHashMap<>();
    private static final Map<String, Object> KV_CUSTOM = new ConcurrentHashMap<>();

    /**
     * Initialize (or re-initialize) all config items, clearing previous caches.
     */
    public static void initialize(List<ConfigItem> items) {
        ALL.clear();

        KV_INT.clear();
        KV_LONG.clear();
        KV_BOOL.clear();
        KV_DECIMAL.clear();
        KV_STR.clear();
        KV_STR_LIST.clear();
        KV_STR_SET.clear();
        KV_CUSTOM.clear();

        if (items == null || items.isEmpty()) {
            log.info("Loaded 0 config items");
            return;
        }

        for (ConfigItem si : items) {
            if (si == null || si.key == null || si.key.isBlank()) {
                continue;
            }

            String key = si.key.trim();
            String raw = si.value;
            ConfigValueType type = Objects.requireNonNullElse(si.type, ConfigValueType.STR);

            ALL.put(key, si);

            switch (type) {
                case INT -> KV_INT.put(key, ConfigValueParser.parseInt(key, raw));
                case LONG -> KV_LONG.put(key, ConfigValueParser.parseLong(key, raw));
                case BOOL -> KV_BOOL.put(key, ConfigValueParser.parseBool(key, raw));
                case DEC -> KV_DECIMAL.put(key, ConfigValueParser.parseDec(key, raw));
                case STR -> KV_STR.put(key, raw);
                case STR_LIST -> KV_STR_LIST.put(key, ConfigValueParser.parseStrList(key, raw));
                case STR_SET -> KV_STR_SET.put(key, ConfigValueParser.parseStrSet(key, raw));
                default -> throw new RuntimeException(String.format("Unknown config type %s (key: %s)", type, key));
            }
        }
        log.info("Loaded {} config items", items.size());
    }

    /**
     * Dynamically refresh a single config item: update the full record and clear
     * all per-key typed caches, then parse and re-cache according to its type.
     */
    public static void refresh(ConfigItem item) {
        if (item == null || item.key == null || item.key.isBlank()) {
            throw new SysException("Refresh config failed: key must not be empty", BaseError.SERVICE_ERROR);
        }

        String key = item.key.trim();
        String value = item.value;
        ConfigValueType type = Objects.requireNonNullElse(item.type, ConfigValueType.STR);

        ALL.put(key, item);

        KV_STR.remove(key);
        KV_INT.remove(key);
        KV_LONG.remove(key);
        KV_BOOL.remove(key);
        KV_DECIMAL.remove(key);
        KV_STR_LIST.remove(key);
        KV_STR_SET.remove(key);
        KV_CUSTOM.remove(key);

        switch (type) {
            case STR -> KV_STR.put(key, value);
            case INT -> KV_INT.put(key, ConfigValueParser.parseInt(key, value));
            case LONG -> KV_LONG.put(key, ConfigValueParser.parseLong(key, value));
            case BOOL -> KV_BOOL.put(key, ConfigValueParser.parseBool(key, value));
            case DEC -> KV_DECIMAL.put(key, ConfigValueParser.parseDec(key, value));
            case STR_LIST -> KV_STR_LIST.put(key, ConfigValueParser.parseStrList(key, value));
            case STR_SET -> KV_STR_SET.put(key, ConfigValueParser.parseStrSet(key, value));
            default -> {
                String msg = String.format("Unknown config type %s (key: %s)", type, key);
                throw new SysException(msg, BaseError.SERVICE_ERROR);
            }
        }

        log.info("Refreshed config {}={}({})", key, type, value);
    }

    public static int intV(ConfigDefinition def) {
        return getRequiredType(def, KV_INT, raw -> ConfigValueParser.parseInt(def.name(), raw));
    }

    public static long longV(ConfigDefinition def) {
        return getRequiredType(def, KV_LONG, raw -> ConfigValueParser.parseLong(def.name(), raw));
    }

    public static boolean booleanV(ConfigDefinition def) {
        return getRequiredType(def, KV_BOOL, raw -> ConfigValueParser.parseBool(def.name(), raw));
    }

    public static BigDecimal decimalV(ConfigDefinition def) {
        return getRequiredType(def, KV_DECIMAL, raw -> ConfigValueParser.parseDec(def.name(), raw));
    }

    public static String stringV(ConfigDefinition def) {
        return getRequiredType(def, KV_STR, raw -> raw);
    }

    public static List<String> strListV(ConfigDefinition def) {
        return getRequiredType(def, KV_STR_LIST, raw -> ConfigValueParser.parseStrList(def.name(), raw));
    }

    public static Set<String> strSetV(ConfigDefinition def) {
        return getRequiredType(def, KV_STR_SET, raw -> ConfigValueParser.parseStrSet(def.name(), raw));
    }

    public static <T> T customV(ConfigDefinition def, Function<String, T> mapper) {
        return customV0(def, mapper);
    }

    private static ConfigItem getRequiredItem(String key) {
        ConfigItem si = ALL.get(key);
        if (si == null) {
            throw new SysException("Missing config item: " + key, BaseError.SERVICE_ERROR);
        }
        return si;
    }

    private static void assertTypeMatch(String key, ConfigValueType expected, ConfigItem si) {
        if (si.type != expected) {
            String msg = String.format("Config type mismatch: key=%s, expected=%s, actual=%s", key, expected, si.type);
            throw new SysException(msg, BaseError.SERVICE_ERROR);
        }
    }

    private static <R> R getRequiredType(ConfigDefinition def, Map<String, R> typedCache, Function<String, R> parser) {
        String k = def.name();

        // 1) Type-cache hit
        R cached = typedCache.get(k);
        if (cached != null) {
            return cached;
        }

        // 2) Validate existence and type against the full record
        ConfigItem si = getRequiredItem(k);
        assertTypeMatch(k, def.valueType(), si);

        // 3) Parse and populate cache (computeIfAbsent prevents duplicate concurrent parsing)
        return typedCache.computeIfAbsent(k, __ -> parser.apply(si.value));
    }

    @SuppressWarnings("unchecked")
    private static <T> T customV0(ConfigDefinition def, Function<String, T> mapper) {
        String k = def.name();
        Object cached = KV_CUSTOM.computeIfAbsent(k, __ -> {
            String raw = stringV(def);
            T obj = mapper.apply(raw);
            log.info("Cached custom config {}={}", k, raw);
            return obj;
        });
        return (T) cached;
    }

    private ConfigRegistry() {
    }

}


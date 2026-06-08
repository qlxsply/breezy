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
 * 配置值容器。
 * <p>
 * 用于在内存中存放系统配置项，支持多种类型（字符串、数值、布尔、小数等）。
 * 通过静态方法读取或更新配置，线程安全。
 * </p>
 *
 * @author Corwin 2025/10/14
 */
@Slf4j
public class ConfigRegistry {
    // 全量配置记录
    private static final Map<String, ConfigItem> ALL = new ConcurrentHashMap<>();
    // 各种类型的配置缓存，按类型分开存储
    private static final Map<String, Integer> KV_INT = new ConcurrentHashMap<>();
    private static final Map<String, Long> KV_LONG = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> KV_BOOL = new ConcurrentHashMap<>();
    private static final Map<String, BigDecimal> KV_DECIMAL = new ConcurrentHashMap<>();
    private static final Map<String, String> KV_STR = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> KV_STR_LIST = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> KV_STR_SET = new ConcurrentHashMap<>();
    private static final Map<String, Object> KV_CUSTOM = new ConcurrentHashMap<>();

    /**
     * 初始化配置项。
     * 清空旧的缓存，然后根据传入的配置列表填充对应类型的映射。
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
            log.info("已加载 0 条配置项");
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
                default -> throw new RuntimeException(String.format("未知的配置类型 %s (键: %s)", type, key));
            }
        }
        log.info("已加载 {} 条配置项", items.size());
    }

    /**
     * 动态刷新单个配置项：更新全量记录，并清除该 key 的各类型缓存与自定义缓存。
     */
    public static void refresh(ConfigItem item) {
        if (item == null || item.key == null || item.key.isBlank()) {
            throw new SysException("刷新配置失败：key 不能为空", BaseError.SERVICE_ERROR);
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
                String msg = String.format("未知的配置类型 %s (键: %s)", type, key);
                throw new SysException(msg, BaseError.SERVICE_ERROR);
            }
        }

        log.info("已刷新配置 {}={}({})", key, type, value);
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
            throw new SysException("缺少配置项: " + key, BaseError.SERVICE_ERROR);
        }
        return si;
    }

    private static void assertTypeMatch(String key, ConfigValueType expected, ConfigItem si) {
        if (si.type != expected) {
            String msg = String.format("配置项类型不一致: key=%s, expected=%s, actual=%s", key, expected, si.type);
            throw new SysException(msg, BaseError.SERVICE_ERROR);
        }
    }

    private static <R> R getRequiredType(ConfigDefinition def, Map<String, R> typedCache, Function<String, R> parser) {
        String k = def.name();

        // 1) 类型缓存命中直接返回
        R cached = typedCache.get(k);
        if (cached != null) {
            return cached;
        }

        // 2) 全量记录校验存在性与类型
        ConfigItem si = getRequiredItem(k);
        assertTypeMatch(k, def.valueType(), si);

        // 3) 解析并回填缓存
        // 这里使用 computeIfAbsent 防止并发重复解析
        return typedCache.computeIfAbsent(k, __ -> parser.apply(si.value));
    }

    @SuppressWarnings("unchecked")
    private static <T> T customV0(ConfigDefinition def, Function<String, T> mapper) {
        String k = def.name();
        Object cached = KV_CUSTOM.computeIfAbsent(k, __ -> {
            String raw = stringV(def);
            T obj = mapper.apply(raw);
            log.info("已缓存自定义配置 {}={}", k, raw);
            return obj;
        });
        return (T) cached;
    }

    private ConfigRegistry() {
    }

}


package com.corwin.framework.event.group;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Objects;

/**
 * 默认消费组解析器。
 * <p>
 * 实现策略：
 * <ul>
 *     <li>对应用名执行字符归一化，确保可用于 Kafka group.id / RabbitMQ queue 命名。</li>
 *     <li>对原始组标识计算短哈希，避免因方法签名过长导致命名超限。</li>
 *     <li>最终格式固定为 {@code app.ae.hash}，并限制最大长度。</li>
 * </ul>
 * 该策略重点保障“稳定且可跨实例复现”，而不是可读性。
 *
 * @author Corwin 2026/4/9
 */
public class DefaultConsumerGroupResolver implements ConsumerGroupResolver {

    private static final int MAX_GROUP_LENGTH = 80;

    /**
     * 将原始组标识解析为最终消费组名称。
     */
    @Override
    public String resolveGroup(String rawGroup, String applicationName) {
        String raw = normalizeRaw(rawGroup);
        String app = normalizeValue(applicationName);
        String hashed = app + ".ae." + shortHash(raw);
        if (hashed.length() <= MAX_GROUP_LENGTH) {
            return hashed;
        }
        return hashed.substring(0, MAX_GROUP_LENGTH);
    }

    /**
     * 校验并归一原始组标识。
     */
    private String normalizeRaw(String rawGroup) {
        if (rawGroup == null || rawGroup.isBlank()) {
            throw new IllegalArgumentException("rawGroup required");
        }
        return rawGroup.trim();
    }

    /**
     * 将输入文本归一化为小写安全字符集（a-z0-9._-）。
     */
    private String normalizeValue(String value) {
        String text = (value == null || value.isBlank()) ? "application" : value.trim();
        StringBuilder builder = new StringBuilder(text.length());
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_') {
                builder.append(c);
            } else {
                builder.append('-');
            }
        }
        String normalized = builder.toString().replaceAll("-{2,}", "-");
        if (normalized.isBlank()) {
            return "application";
        }
        return normalized;
    }

    /**
     * 计算固定长度短哈希，用作消费组后缀。
     */
    private String shortHash(String raw) {
        Objects.requireNonNull(raw, "raw required");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(String.format("%02x", bytes[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is required", ex);
        }
    }
}


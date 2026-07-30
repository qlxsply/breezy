package com.corwin.framework.event.transport.kafka;

import com.corwin.framework.event.config.AsyncEventProperties;

import java.util.Locale;
import java.util.Objects;

/**
 * Default Kafka topic resolver.
 * <p>
 * Rule: {@code normalized(topicPrefix) + "." + normalized(eventType)}.
 * Normalisation replaces illegal characters with dots and collapses consecutive separators.
 *
 * @author Corwin 2026/4/10
 */
public class DefaultEventTopicResolver implements EventTopicResolver {

    private final AsyncEventProperties properties;

    public DefaultEventTopicResolver(AsyncEventProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties required");
    }

    /**
     * 解析事件类型对应的 topic。
     */
    @Override
    public String resolveTopic(String eventType) {
        String prefix = properties.getKafka().getTopicPrefix();
        String normalizedPrefix = normalize(prefix == null || prefix.isBlank() ? "framework.async.event" : prefix);
        String normalizedType = normalize(eventType == null ? "unknown" : eventType);
        return normalizedPrefix + "." + normalizedType;
    }

    /**
     * 归一化 topic 片段，仅保留小写字母、数字及 .-_。
     */
    private String normalize(String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_') {
                builder.append(c);
            } else {
                builder.append('.');
            }
        }
        String result = builder.toString().replaceAll("\\.{2,}", ".");
        if (result.startsWith(".")) {
            result = result.substring(1);
        }
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isBlank() ? "unknown" : result;
    }
}


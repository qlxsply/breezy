package com.corwin.system.resource.infrastructure.sort;

import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.sort.SortRule;
import com.corwin.framework.web.sort.SortableField;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API 排序配置 JSON 解析器。
 *
 * @author Corwin 2026/7/29
 */
@Component
@RequiredArgsConstructor
public class ApiSortOptionsParser {

    private static final String IDENTIFIER_PATTERN = "[A-Za-z_][A-Za-z0-9_]*";
    private static final String COLUMN_PATTERN = "[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?";

    private final ObjectMapper objectMapper;

    public String normalize(String json) {
        String normalized = StrUtil.trimToNull(json);
        if (normalized == null) {
            return null;
        }
        parse(normalized);
        return normalized;
    }

    public SortRule parse(String json) {
        String normalized = StrUtil.trimToNull(json);
        if (normalized == null) {
            return SortRule.disabled();
        }
        ApiSortOptionsPayload payload;
        try {
            payload = objectMapper.readValue(normalized, ApiSortOptionsPayload.class);
        } catch (JsonProcessingException e) {
            throw new BizException("Invalid sort options JSON", BaseError.INVALID_PARAMETER);
        }

        boolean enabled = payload.enabled() == null || payload.enabled();
        if (!enabled) {
            return SortRule.disabled();
        }
        Map<String, String> allowed = parseAllowed(payload.allowed());
        if (allowed.isEmpty()) {
            throw new BizException("Sort allowed fields required", BaseError.INVALID_PARAMETER);
        }
        List<SortSpec> defaults = parseDefaults(payload.defaults(), allowed);
        List<SortableField> allowedFields = allowed.entrySet().stream()
                .map(item -> new SortableField(item.getKey(), item.getValue())).toList();
        return new SortRule(true, allowedFields, defaults);
    }

    private Map<String, String> parseAllowed(List<ApiSortableFieldPayload> allowed) {
        Map<String, String> result = new LinkedHashMap<>();
        if (allowed == null) {
            return result;
        }
        for (ApiSortableFieldPayload item : allowed) {
            if (item == null) {
                continue;
            }
            String field = StrUtil.trimToNull(item.field());
            String column = StrUtil.trimToNull(item.column());
            validateIdentifier(field, "Invalid sort field: ");
            validateColumn(column);
            if (result.putIfAbsent(field, column) != null) {
                throw new BizException("Duplicate sort field: " + field, BaseError.INVALID_PARAMETER);
            }
        }
        return result;
    }

    private List<SortSpec> parseDefaults(List<ApiDefaultSortPayload> defaults, Map<String, String> allowed) {
        List<SortSpec> result = new ArrayList<>();
        if (defaults == null) {
            return result;
        }
        for (ApiDefaultSortPayload item : defaults) {
            if (item == null) {
                continue;
            }
            String field = StrUtil.trimToNull(item.field());
            validateIdentifier(field, "Invalid default sort field: ");
            if (!allowed.containsKey(field)) {
                throw new BizException("Default sort field is not allowed: " + field, BaseError.INVALID_PARAMETER);
            }
            result.add(new SortSpec(field, parseDirection(item.direction())));
        }
        return result;
    }

    private SortDirection parseDirection(String direction) {
        String normalized = StrUtil.trimToNull(direction);
        if (normalized == null) {
            return SortDirection.ASC;
        }
        try {
            return SortDirection.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("Invalid sort direction: " + direction, BaseError.INVALID_PARAMETER);
        }
    }

    private void validateIdentifier(String value, String prefix) {
        if (value == null || !value.matches(IDENTIFIER_PATTERN)) {
            throw new BizException(prefix + value, BaseError.INVALID_PARAMETER);
        }
    }

    private void validateColumn(String value) {
        if (value == null || !value.matches(COLUMN_PATTERN)) {
            throw new BizException("Invalid sort column: " + value, BaseError.INVALID_PARAMETER);
        }
    }

    private record ApiSortOptionsPayload(
            Boolean enabled,
            List<ApiSortableFieldPayload> allowed,
            List<ApiDefaultSortPayload> defaults
    ) {
    }

    private record ApiSortableFieldPayload(
            String field,
            String column
    ) {
    }

    private record ApiDefaultSortPayload(
            String field,
            String direction
    ) {
    }
}

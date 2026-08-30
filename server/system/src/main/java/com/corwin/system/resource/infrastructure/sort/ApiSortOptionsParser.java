package com.corwin.system.resource.infrastructure.sort;

import com.corwin.framework.domain.page.SortDirection;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.framework.json.Json;
import com.corwin.framework.util.StrUtil;
import com.corwin.framework.web.sort.SortRule;
import com.corwin.framework.web.sort.SortableField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for API sort configuration JSON.
 *
 * <p>Validates, normalizes, and parses the sort options JSON associated with an API, producing a
 * {@link SortRule} that defines allowed sort fields and default sort orders.
 *
 * @author Corwin 2026/7/29
 */
public final class ApiSortOptionsParser {

  private static final String IDENTIFIER_PATTERN = "[A-Za-z_][A-Za-z0-9_]*";
  private static final String COLUMN_PATTERN = "[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?";

  private ApiSortOptionsParser() {}

  /**
   * Validates and normalizes a sort options JSON string.
   *
   * @param json the raw JSON string
   * @return the trimmed JSON string if valid, or null if blank
   */
  public static String normalize(String json) {
    String normalized = StrUtil.trimToNull(json);
    if (normalized == null) {
      return null;
    }
    parse(normalized);
    return normalized;
  }

  /**
   * Parses a sort options JSON string into a {@link SortRule}.
   *
   * @param json the sort options JSON string
   * @return the parsed SortRule, or a disabled rule if the input is blank or sorting is disabled
   * @throws BizException if the JSON structure is invalid
   */
  public static SortRule parse(String json) {
    String normalized = StrUtil.trimToNull(json);
    if (normalized == null) {
      return SortRule.disabled();
    }
    ApiSortOptionsPayload payload = Json.parse(normalized, ApiSortOptionsPayload.class);

    boolean enabled = payload.enabled() == null || payload.enabled();
    if (!enabled) {
      return SortRule.disabled();
    }
    Map<String, String> allowed = parseAllowed(payload.allowed());
    if (allowed.isEmpty()) {
      throw new BizException("Sort allowed fields required", BaseError.INVALID_PARAMETER);
    }
    List<SortSpec> defaults = parseDefaults(payload.defaults(), allowed);
    List<SortableField> allowedFields =
        allowed.entrySet().stream()
            .map(item -> new SortableField(item.getKey(), item.getValue()))
            .toList();
    return new SortRule(true, allowedFields, defaults);
  }

  /**
   * Parses the allowed fields list into a field-to-column mapping.
   *
   * @param allowed the list of allowed sort field payloads
   * @return a map of field name to column expression
   */
  private static Map<String, String> parseAllowed(List<ApiSortableFieldPayload> allowed) {
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

  /**
   * Parses the default sort specifications from the payload.
   *
   * @param defaults the list of default sort payloads
   * @param allowed the allowed field-to-column mapping for validation
   * @return the list of default sort specifications
   */
  private static List<SortSpec> parseDefaults(
      List<ApiDefaultSortPayload> defaults, Map<String, String> allowed) {
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
        throw new BizException(
            "Default sort field is not allowed: " + field, BaseError.INVALID_PARAMETER);
      }
      result.add(new SortSpec(field, parseDirection(item.direction())));
    }
    return result;
  }

  /**
   * Parses a sort direction string into a {@link SortDirection} enum.
   *
   * @param direction the direction string ("ASC" or "DESC")
   * @return the corresponding SortDirection, defaulting to ASC if blank
   */
  private static SortDirection parseDirection(String direction) {
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

  /**
   * Validates that a string is a valid identifier.
   *
   * @param value the identifier to validate
   * @param prefix the error message prefix
   */
  private static void validateIdentifier(String value, String prefix) {
    if (value == null || !value.matches(IDENTIFIER_PATTERN)) {
      throw new BizException(prefix + value, BaseError.INVALID_PARAMETER);
    }
  }

  /**
   * Validates that a string is a valid column reference.
   *
   * @param value the column reference to validate
   */
  private static void validateColumn(String value) {
    if (value == null || !value.matches(COLUMN_PATTERN)) {
      throw new BizException("Invalid sort column: " + value, BaseError.INVALID_PARAMETER);
    }
  }

  private record ApiSortOptionsPayload(
      Boolean enabled,
      List<ApiSortableFieldPayload> allowed,
      List<ApiDefaultSortPayload> defaults) {}

  private record ApiSortableFieldPayload(String field, String column) {}

  private record ApiDefaultSortPayload(String field, String direction) {}
}

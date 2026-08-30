package com.corwin.system.config.application.service;

import com.corwin.framework.config.codec.ConfigJsonCodec;
import com.corwin.framework.config.definition.ConfigActivationPolicy;
import com.corwin.framework.config.definition.ConfigEditPolicy;
import com.corwin.framework.config.definition.ConfigSpec;
import com.corwin.framework.config.definition.ConfigSpecCatalog;
import com.corwin.framework.config.runtime.ConfigRegistry;
import com.corwin.framework.config.runtime.ConfigSnapshot;
import com.corwin.framework.config.runtime.ConfigValueSource;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizException;
import com.corwin.system.config.application.view.ConfigEffectiveView;
import com.corwin.system.config.application.view.ConfigManagementStatus;
import com.corwin.system.config.application.view.ConfigManagementView;
import com.corwin.system.config.domain.model.ConfigValue;
import com.corwin.system.config.domain.repo.ConfigValueRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Corwin 2026/7/30
 */
@Service
@RequiredArgsConstructor
public class ConfigQueryService {

  private final ConfigValueRepository configValueRepository;

  @Transactional(readOnly = true)
  public PageData<ConfigManagementView> page(
      String keyword, String module, String group, PageSpec pageSpec) {
    PageSpec effectivePageSpec = PageSpec.ensure(pageSpec);
    Map<String, ConfigValue> storedValues =
        configValueRepository.findAll().stream()
            .collect(Collectors.toMap(ConfigValue::configKey, Function.identity()));
    String normalizedKeyword = normalize(keyword);
    String normalizedModule = normalize(module);
    String normalizedGroup = normalize(group);

    List<ConfigManagementView> matched =
        ConfigSpecCatalog.loadAll().values().stream()
            .filter(spec -> matches(spec, normalizedKeyword, normalizedModule, normalizedGroup))
            .sorted(specComparator())
            .map(spec -> toView(spec, storedValues.get(spec.key().value())))
            .toList();

    long offset =
        Math.max(0L, (long) (effectivePageSpec.pageNo() - 1) * effectivePageSpec.pageSize());
    int fromIndex = (int) Math.min(offset, matched.size());
    int toIndex = Math.min(fromIndex + effectivePageSpec.pageSize(), matched.size());
    return PageData.of(effectivePageSpec, matched.size(), matched.subList(fromIndex, toIndex));
  }

  @Transactional(readOnly = true)
  public ConfigManagementView detail(String configKey) {
    ConfigSpec<?> spec = requireSpec(configKey);
    ConfigValue storedValue =
        configValueRepository.findByConfigKey(spec.key().value()).orElse(null);
    return toView(spec, storedValue);
  }

  @Transactional(readOnly = true)
  public ConfigEffectiveView effective(String configKey) {
    ConfigSpec<?> spec = requireSpec(configKey);
    ConfigSnapshot<?> snapshot = ConfigRegistry.snapshot(spec);
    JsonNode effectiveValue =
        ConfigValueTreeSupport.redact(ConfigJsonCodec.valueToTree(snapshot.value()), spec.fields());
    return new ConfigEffectiveView(
        spec.key().value(), spec.title(), spec.description(), effectiveValue);
  }

  private boolean matches(ConfigSpec<?> spec, String keyword, String module, String group) {
    if (!module.isEmpty() && !normalize(spec.module()).equals(module)) {
      return false;
    }
    if (!group.isEmpty() && !normalize(spec.group()).equals(group)) {
      return false;
    }
    if (keyword.isEmpty()) {
      return true;
    }
    return normalize(spec.key().value()).contains(keyword)
        || normalize(spec.title()).contains(keyword)
        || normalize(spec.description()).contains(keyword);
  }

  private Comparator<ConfigSpec<?>> specComparator() {
    return Comparator.comparing(ConfigSpec<?>::module)
        .thenComparing(ConfigSpec::group)
        .thenComparingInt(ConfigSpec::order)
        .thenComparing(spec -> spec.key().value());
  }

  private <T> ConfigManagementView toView(ConfigSpec<T> spec, ConfigValue storedValue) {
    ConfigSnapshot<T> snapshot = ConfigRegistry.snapshot(spec);
    JsonNode effectiveValue = ConfigJsonCodec.valueToTree(snapshot.value());
    JsonNode defaultValue = ConfigJsonCodec.valueToTree(spec.defaultValue());
    PersistedTree persisted = persistedTree(spec, storedValue, defaultValue);
    long persistedRevision = storedValue == null ? 0 : storedValue.revision();
    boolean configured = storedValue != null && storedValue.configured();
    boolean pendingRestart =
        spec.activationPolicy() == ConfigActivationPolicy.RESTART_REQUIRED
            && storedValue != null
            && persistedRevision != snapshot.revision();

    return new ConfigManagementView(
        spec.key().value(),
        spec.module(),
        spec.group(),
        spec.title(),
        spec.description(),
        spec.schemaVersion(),
        persistedRevision,
        snapshot.revision(),
        configured,
        spec.activationPolicy(),
        spec.editPolicy(),
        snapshot.source(),
        status(spec, snapshot, configured, pendingRestart),
        pendingRestart,
        ConfigValueTreeSupport.redact(effectiveValue, spec.fields()),
        ConfigValueTreeSupport.redact(persisted.value(), spec.fields()),
        ConfigValueTreeSupport.redact(defaultValue, spec.fields()),
        spec.fields(),
        ConfigValueTreeSupport.sensitivePresence(persisted.presenceSource(), spec.fields()),
        spec.editorId(),
        snapshot.loadWarning());
  }

  private <T> PersistedTree persistedTree(
      ConfigSpec<T> spec, ConfigValue storedValue, JsonNode defaultValue) {
    if (storedValue == null || !storedValue.configured()) {
      return new PersistedTree(defaultValue, defaultValue);
    }
    JsonNode rawTree;
    try {
      rawTree = ConfigJsonCodec.readTree(storedValue.content());
    } catch (RuntimeException ex) {
      return new PersistedTree(NullNode.getInstance(), NullNode.getInstance());
    }
    try {
      T typedValue = ConfigJsonCodec.treeToValue(rawTree, spec.valueClass());
      return new PersistedTree(ConfigJsonCodec.valueToTree(typedValue), rawTree);
    } catch (RuntimeException ex) {
      return new PersistedTree(NullNode.getInstance(), rawTree);
    }
  }

  private ConfigManagementStatus status(
      ConfigSpec<?> spec, ConfigSnapshot<?> snapshot, boolean configured, boolean pendingRestart) {
    if (snapshot.source() == ConfigValueSource.INVALID_DATABASE_FALLBACK) {
      return ConfigManagementStatus.INVALID_DATABASE_VALUE;
    }
    if (pendingRestart) {
      return ConfigManagementStatus.RESTART_REQUIRED;
    }
    if (spec.editPolicy() == ConfigEditPolicy.READ_ONLY) {
      return ConfigManagementStatus.READ_ONLY;
    }
    return configured ? ConfigManagementStatus.CONFIGURED : ConfigManagementStatus.DEFAULT_VALUE;
  }

  private ConfigSpec<?> requireSpec(String configKey) {
    return ConfigSpecCatalog.find(configKey)
        .orElseThrow(
            () ->
                new BizException("Config definition not found: " + configKey, BaseError.NOT_FOUND));
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private record PersistedTree(JsonNode value, JsonNode presenceSource) {}
}

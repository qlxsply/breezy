package com.corwin.system.user.application.service;

import com.corwin.framework.config.runtime.Configs;
import com.corwin.system.dict.application.service.DictQueryService;
import com.corwin.system.user.application.view.UserConfigView;
import com.corwin.system.user.config.SystemUserConfigSpecs;
import com.corwin.system.user.config.UserConfigCodes;
import com.corwin.system.user.domain.model.UserConfig;
import com.corwin.system.user.domain.repo.UserConfigRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Corwin 2026/3/30
 */
@Service
@RequiredArgsConstructor
public class UserConfigAppService {

  private static final Set<String> PERSONALIZED_CODES =
      Set.of(
          UserConfigCodes.TIME_ZONE,
          UserConfigCodes.DATE_TIME_FORMAT,
          UserConfigCodes.DATE_FORMAT,
          UserConfigCodes.DECIMAL_FORMAT,
          UserConfigCodes.ADMIN_TAB_KEEP_ALIVE);

  private final UserConfigRepository userConfigRepository;
  private final DictQueryService dictQueryService;

  public List<UserConfigView> getMergedConfigs(Long userId) {
    Map<String, String> defaults = publicDefaults();
    Map<String, String> overrides =
        userId == null
            ? Map.of()
            : userConfigRepository.findByUserId(userId).stream()
                .collect(
                    Collectors.toMap(
                        UserConfig::getConfigCode,
                        UserConfig::getConfigValue,
                        (left, right) -> left));
    return defaults.entrySet().stream()
        .map(
            entry ->
                new UserConfigView(
                    entry.getKey(),
                    description(entry.getKey()),
                    valueType(entry.getKey()),
                    overrides.getOrDefault(entry.getKey(), entry.getValue())))
        .toList();
  }

  @Transactional
  public void updateMyConfig(Long userId, String code, String value) {
    if (userId == null) {
      throw new IllegalArgumentException("游客不支持更新个性化设置");
    }
    if (!PERSONALIZED_CODES.contains(code)) {
      throw new IllegalArgumentException("不支持更新该配置项: " + code);
    }
    String normalizedValue = value == null ? "" : value.trim();
    validateValue(code, normalizedValue);
    UserConfig userConfig =
        userConfigRepository
            .findByUserIdAndConfigCode(userId, code)
            .orElse(new UserConfig(userId, code, normalizedValue));
    userConfig.updateValue(normalizedValue);
    userConfigRepository.save(userConfig);
  }

  private Map<String, String> publicDefaults() {
    var defaults = Configs.get(SystemUserConfigSpecs.USER_PREFERENCE_DEFAULTS);
    var values = new LinkedHashMap<String, String>();
    values.put(UserConfigCodes.TIME_ZONE, defaults.timeZone().itemValue());
    values.put(UserConfigCodes.DATE_TIME_FORMAT, defaults.dateTimeFormat().itemValue());
    values.put(UserConfigCodes.DATE_FORMAT, defaults.dateFormat().itemValue());
    values.put(UserConfigCodes.DECIMAL_FORMAT, defaults.decimalFormat().itemValue());
    values.put(
        UserConfigCodes.ADMIN_TAB_KEEP_ALIVE, Boolean.toString(defaults.adminTabKeepAlive()));
    return values;
  }

  private void validateValue(String code, String value) {
    if (UserConfigCodes.ADMIN_TAB_KEEP_ALIVE.equals(code)) {
      if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
        throw new IllegalArgumentException("标签页状态记忆配置必须为布尔值");
      }
      return;
    }
    dictQueryService.assertValidValue(code, value);
  }

  private String valueType(String code) {
    return UserConfigCodes.ADMIN_TAB_KEEP_ALIVE.equals(code) ? "BOOL" : "STR";
  }

  private String description(String code) {
    return switch (code) {
      case UserConfigCodes.TIME_ZONE -> "用户默认时区";
      case UserConfigCodes.DATE_TIME_FORMAT -> "用户默认日期时间格式";
      case UserConfigCodes.DATE_FORMAT -> "用户默认日期格式";
      case UserConfigCodes.DECIMAL_FORMAT -> "用户默认数字符号组合";
      case UserConfigCodes.ADMIN_TAB_KEEP_ALIVE -> "记忆后台标签页状态";
      default -> code;
    };
  }
}

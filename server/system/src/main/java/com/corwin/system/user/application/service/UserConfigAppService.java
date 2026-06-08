package com.corwin.system.user.application.service;

import com.corwin.framework.config.*;
import com.corwin.system.config.application.validation.ConfigValueValidator;
import com.corwin.system.user.application.view.UserConfigView;
import com.corwin.system.user.domain.model.UserConfig;
import com.corwin.system.user.domain.repo.UserConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户配置应用服务 (实现 Overlay 覆盖逻辑)
 *
 * @author Corwin 2026/3/30
 */
@Service
@RequiredArgsConstructor
public class UserConfigAppService {

    private static final Set<String> PERSONALIZED_CODES = new LinkedHashSet<>(
            List.of(DefaultConfigKeys.USER_TIME_ZONE.name(), DefaultConfigKeys.USER_DATE_TIME_FORMAT.name(),
                    DefaultConfigKeys.USER_DATE_FORMAT.name(), DefaultConfigKeys.USER_DECIMAL_FORMAT.name()));

    private final UserConfigRepository userConfigRepository;
    private final ConfigStore configStore;
    private final List<ConfigValueValidator> validators;


    /**
     * 获取用户合并后的配置 (用户覆盖值 > 系统默认值)
     * 支持 userId 为 null (游客模式)
     */
    public List<UserConfigView> getMergedConfigs(Long userId) {
        List<StoredConfig> configs = configStore.findByLevel(ConfigLevel.USER);
        if (configs.isEmpty()) {
            return List.of();
        }

        Map<String, StoredConfig> configMap = configs.stream()
                .collect(Collectors.toMap(StoredConfig::code, item -> item, (left, right) -> left));

        List<StoredConfig> orderedConfigs = PERSONALIZED_CODES.stream().map(configMap::get).filter(item -> item != null)
                .toList();

        if (orderedConfigs.isEmpty()) {
            return List.of();
        }

        if (userId == null) {
            return orderedConfigs.stream()
                    .map(config -> new UserConfigView(config.code(), config.description(), config.valueType(),
                            config.value())).toList();
        }

        List<UserConfig> overrides = userConfigRepository.findByUserId(userId);
        Map<String, String> overrideMap = overrides.stream().collect(
                Collectors.toMap(UserConfig::getConfigCode, UserConfig::getConfigValue, (left, right) -> left));

        return orderedConfigs.stream().map(config -> {
            String value = overrideMap.getOrDefault(config.code(), config.value());
            return new UserConfigView(config.code(), config.description(), config.valueType(), value);
        }).toList();
    }

    /**
     * 更新用户个性化配置
     */
    @Transactional
    public void updateMyConfig(Long userId, String code, String value) {
        if (userId == null) {
            throw new IllegalArgumentException("游客不支持更新个性化设置");
        }
        if (!PERSONALIZED_CODES.contains(code)) {
            throw new IllegalArgumentException("不支持更新该配置项: " + code);
        }
        StoredConfig config = configStore.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("配置项不存在: " + code));
        if (!config.personalized()) {
            throw new IllegalArgumentException("该配置项不支持个性化设置: " + code);
        }
        validateValue(config, value);

        UserConfig userConfig = userConfigRepository.findByUserIdAndConfigCode(userId, code)
                .orElse(new UserConfig(userId, code, value));
        userConfig.updateValue(value);
        userConfigRepository.save(userConfig);
    }

    private void validateValue(StoredConfig config, String rawValue) {
        if (validators == null || validators.isEmpty()) {
            return;
        }
        for (ConfigValueValidator validator : validators) {
            if (validator.supports(config)) {
                validator.validate(config, rawValue);
            }
        }
    }
}

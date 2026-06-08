package com.corwin.system.config.application.service;

import com.corwin.framework.config.ConfigDefinitionCatalog;
import com.corwin.framework.config.ConfigStore;
import com.corwin.framework.config.StoredConfig;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.ClientIpMode;
import com.corwin.framework.util.HighDate;
import com.corwin.framework.util.IpUtil;
import com.corwin.system.config.application.command.UpdateConfigValueCommand;
import com.corwin.system.config.application.validation.ConfigValueValidator;
import com.corwin.system.config.application.view.ConfigClientIpPreviewView;
import com.corwin.system.config.application.view.ConfigTimeOffsetPreviewView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * @author Corwin 2026/5/5
 */
@Service
@RequiredArgsConstructor
public class ConfigAdminService {

    private final ConfigStore configStore;
    private final List<ConfigValueValidator> validators;

    public List<StoredConfig> query(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return configStore.findAllActive();
        }
        return configStore.findByKeyword(keyword.trim());
    }

    public PageData<StoredConfig> page(String keyword, PageSpec spec) {
        PageSpec safeSpec = spec == null ? PageSpec.of(1, 20, List.of()) : spec;
        List<StoredConfig> matched = query(keyword);
        int total = matched.size();
        int fromIndex = Math.min(Math.max(0, (safeSpec.pageNo() - 1) * safeSpec.pageSize()), total);
        int toIndex = Math.min(fromIndex + safeSpec.pageSize(), total);
        return PageData.of(safeSpec.pageNo(), safeSpec.pageSize(), total, matched.subList(fromIndex, toIndex));
    }

    @Transactional
    public boolean updateValue(UpdateConfigValueCommand cmd) {
        BizAssert.notNull(ConfigDefinitionCatalog.findByCode(cmd.code()).orElse(null), BaseError.INVALID_PARAMETER);

        return configStore.findByCode(cmd.code()).map(config -> {
            validateValue(config, cmd.rawValue());
            return configStore.updateValue(cmd.code(), cmd.rawValue());
        }).orElse(false);
    }

    public ConfigClientIpPreviewView previewClientIp(String mode, String remoteAddr, String xRealIp,
            String xForwardedFor, String cfConnectingIp, String trueClientIp) {
        BizAssert.notBlank(mode, BaseError.INVALID_PARAMETER);

        ClientIpMode clientIpMode = parseClientIpMode(mode);
        String resolvedIp = IpUtil.resolveClientIp(clientIpMode, remoteAddr, xRealIp, xForwardedFor, cfConnectingIp,
                trueClientIp);

        return new ConfigClientIpPreviewView(clientIpMode.name(), resolvedIp, remoteAddr, xRealIp, xForwardedFor,
                cfConnectingIp, trueClientIp);
    }

    public ConfigTimeOffsetPreviewView previewTimeOffset(Long offsetSeconds, Long targetEpochMillis) {
        long safeOffsetSeconds = offsetSeconds == null ? 0L : offsetSeconds;
        Instant serverNow = HighDate.realInstant();

        long serverNowEpochMillis = serverNow.toEpochMilli();
        long mockedEpochMillis = serverNow.plusSeconds(safeOffsetSeconds).toEpochMilli();

        Long calculatedOffsetSeconds = null;
        long safeTargetEpochMillis = 0L;
        if (targetEpochMillis != null) {
            safeTargetEpochMillis = targetEpochMillis;
            calculatedOffsetSeconds = Duration.between(serverNow, Instant.ofEpochMilli(targetEpochMillis)).getSeconds();
        }

        return new ConfigTimeOffsetPreviewView(serverNowEpochMillis, safeTargetEpochMillis, calculatedOffsetSeconds,
                safeOffsetSeconds, mockedEpochMillis);
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

    private ClientIpMode parseClientIpMode(String mode) {
        try {
            return ClientIpMode.valueOf(mode.trim());
        } catch (Exception ex) {
            throw new BizException(BaseError.INVALID_PARAMETER);
        }
    }
}

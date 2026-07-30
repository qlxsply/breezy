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
 * Application service for system configuration administration.
 * Provides use cases for querying, paginating, updating configuration values,
 * and previewing client IP resolution and time offset calculations.
 *
 * @author Corwin 2026/5/5
 */
@Service
@RequiredArgsConstructor
public class ConfigAdminService {

    private final ConfigStore configStore;
    private final List<ConfigValueValidator> validators;

    /**
     * Query configurations by optional code and description fuzzy matching.
     *
     * @param codeLike        optional code filter
     * @param descriptionLike optional description filter
     * @return list of matching stored configurations
     */
    public List<StoredConfig> query(String codeLike, String descriptionLike) {
        String normalizedCodeLike = codeLike == null ? "" : codeLike.trim();
        String normalizedDescriptionLike = descriptionLike == null ? "" : descriptionLike.trim();
        if (normalizedCodeLike.isEmpty() && normalizedDescriptionLike.isEmpty()) {
            return configStore.findAllActive();
        }
        String loweredCodeLike = normalizedCodeLike.toLowerCase();
        String loweredDescriptionLike = normalizedDescriptionLike.toLowerCase();
        return configStore.findAllActive().stream()
                .filter(item -> normalizedCodeLike.isEmpty() || item.code().toLowerCase().contains(loweredCodeLike))
                .filter(item -> normalizedDescriptionLike.isEmpty() || item.description().toLowerCase()
                        .contains(loweredDescriptionLike)).toList();
    }

    /**
     * Paginated query of configurations with optional code and description filtering.
     *
     * @param codeLike        optional code filter
     * @param descriptionLike optional description filter
     * @param spec            the page specification
     * @return page of stored configurations
     */
    public PageData<StoredConfig> page(String codeLike, String descriptionLike, PageSpec spec) {
        List<StoredConfig> matched = query(codeLike, descriptionLike);
        int total = matched.size();
        int fromIndex = Math.min(Math.max(0, (spec.pageNo() - 1) * spec.pageSize()), total);
        int toIndex = Math.min(fromIndex + spec.pageSize(), total);
        return PageData.of(spec.pageNo(), spec.pageSize(), total, matched.subList(fromIndex, toIndex));
    }

    /**
     * Update a configuration value after validating it against all applicable validators.
     *
     * @param cmd the command containing the config code and new value
     * @return true if the update was successful
     */
    @Transactional
    public boolean updateValue(UpdateConfigValueCommand cmd) {
        BizAssert.notNull(ConfigDefinitionCatalog.findByCode(cmd.code()).orElse(null), BaseError.INVALID_PARAMETER);

        return configStore.findByCode(cmd.code()).map(config -> {
            validateValue(config, cmd.rawValue());
            return configStore.updateValue(cmd.code(), cmd.rawValue());
        }).orElse(false);
    }

    /**
     * Preview the resolved client IP based on the specified mode and request header values.
     *
     * @param mode           the client IP resolution mode
     * @param remoteAddr     the remote address from the request
     * @param xRealIp        the X-Real-IP header value
     * @param xForwardedFor  the X-Forwarded-For header value
     * @param cfConnectingIp the CF-Connecting-IP header value
     * @param trueClientIp   the True-Client-IP header value
     * @return the client IP preview view
     */
    public ConfigClientIpPreviewView previewClientIp(String mode, String remoteAddr, String xRealIp,
            String xForwardedFor, String cfConnectingIp, String trueClientIp) {
        BizAssert.notBlank(mode, BaseError.INVALID_PARAMETER);

        ClientIpMode clientIpMode = parseClientIpMode(mode);
        String resolvedIp = IpUtil.resolveClientIp(clientIpMode, remoteAddr, xRealIp, xForwardedFor, cfConnectingIp,
                trueClientIp);

        return new ConfigClientIpPreviewView(clientIpMode.name(), resolvedIp, remoteAddr, xRealIp, xForwardedFor,
                cfConnectingIp, trueClientIp);
    }

    /**
     * Preview time offset calculation. If targetEpochMillis is provided, the offset
     * from the current server time is calculated. Otherwise, the mocked time based
     * on offsetSeconds is returned.
     *
     * @param offsetSeconds     the offset in seconds to apply
     * @param targetEpochMillis the target epoch millis to calculate offset from
     * @return the time offset preview view
     */
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

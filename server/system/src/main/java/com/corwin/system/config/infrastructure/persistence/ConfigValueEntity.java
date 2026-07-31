package com.corwin.system.config.infrastructure.persistence;

import com.corwin.system.config.domain.model.ConfigValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author Corwin 2026/7/30
 */
@Getter
@Entity
@Table(name = "sys_config_value")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfigValueEntity {

    @Id
    @Column(name = "config_key", length = 160, nullable = false)
    private String configKey;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "revision", nullable = false)
    private long revision;

    @Column(name = "configured", nullable = false)
    private boolean configured;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "update_reason", length = 500)
    private String updateReason;

    private ConfigValueEntity(ConfigValue value) {
        configKey = value.configKey();
        content = value.content();
        schemaVersion = value.schemaVersion();
        revision = value.revision();
        configured = value.configured();
        updatedBy = value.updatedBy();
        updatedAt = value.updatedAt();
        updateReason = value.updateReason();
    }

    public static ConfigValueEntity fromDomain(ConfigValue value) {
        return new ConfigValueEntity(value);
    }

    public ConfigValue toDomain() {
        return new ConfigValue(configKey, content, schemaVersion, revision, configured, updatedBy, updatedAt,
                updateReason);
    }
}

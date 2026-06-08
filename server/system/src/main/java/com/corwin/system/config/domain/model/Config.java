package com.corwin.system.config.domain.model;

import com.corwin.framework.config.ConfigLevel;
import com.corwin.framework.config.ConfigValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.Objects;

/**
 * @author Corwin 2026/5/5
 */
@Entity
@Table(name = "sys_config")
public class Config {

    @Id
    @Column(length = 128)
    @Getter
    private String code;

    @Column(name = "config_value", nullable = false, length = 4000)
    @Getter
    private String configValue;

    @Column(name = "config_value_type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    @Getter
    private ConfigValueType configValueType;

    @Column(nullable = false, length = 256)
    @Getter
    private String description;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    @Getter
    private ConfigLevel level;

    @Column(nullable = false)
    @Getter
    private boolean expired;

    protected Config() {
    }

    public static Config create(String code, String configValue, ConfigValueType configValueType, String description,
            ConfigLevel level, boolean expired) {
        Config entity = new Config();
        entity.code = Objects.requireNonNull(code, "code required");
        entity.configValue = Objects.requireNonNull(configValue, "configValue required");
        entity.configValueType = Objects.requireNonNull(configValueType, "configValueType required");
        entity.description = Objects.requireNonNull(description, "description required");
        entity.level = Objects.requireNonNull(level, "level required");
        entity.expired = expired;
        return entity;
    }

    public void updateValue(String value) {
        this.configValue = Objects.requireNonNull(value, "value required");
    }
}

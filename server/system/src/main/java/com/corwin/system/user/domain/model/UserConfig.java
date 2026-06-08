package com.corwin.system.user.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.util.Objects;

/**
 * 用户个性化配置
 *
 * @author Corwin 2026/3/30
 */
@Getter
@Entity
@Table(name = "sys_user_config",
       uniqueConstraints = {@UniqueConstraint(name = "uk_user_config", columnNames = {"user_id", "config_code"})})
public class UserConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "config_code", nullable = false, length = 128)
    private String configCode;

    @Column(name = "config_value", nullable = false, length = 4000)
    private String configValue;

    protected UserConfig() {
    }

    public UserConfig(Long userId, String configCode, String configValue) {
        this.userId = Objects.requireNonNull(userId);
        this.configCode = Objects.requireNonNull(configCode);
        this.configValue = Objects.requireNonNull(configValue);
    }

    public void updateValue(String configValue) {
        this.configValue = configValue;
    }
}

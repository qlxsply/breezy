package com.corwin.system.user.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import lombok.Getter;

/**
 * Represents a personalized configuration entry for a user, mapped to the {@code sys_user_config}
 * table with a unique constraint on (user_id, config_code).
 *
 * @author Corwin 2026/3/30
 */
@Getter
@Entity
@Table(
    name = "sys_user_config",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_user_config",
          columnNames = {"user_id", "config_code"})
    })
public class UserConfig {

  /** Primary key, auto-incremented. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The user this configuration entry belongs to. */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** Configuration code/key identifying the setting. */
  @Column(name = "config_code", nullable = false, length = 128)
  private String configCode;

  /** Configuration value associated with the config code. */
  @Column(name = "config_value", nullable = false, length = 4000)
  private String configValue;

  protected UserConfig() {}

  public UserConfig(Long userId, String configCode, String configValue) {
    this.userId = Objects.requireNonNull(userId);
    this.configCode = Objects.requireNonNull(configCode);
    this.configValue = Objects.requireNonNull(configValue);
  }

  /**
   * Updates the configuration value.
   *
   * @param configValue the new configuration value
   */
  public void updateValue(String configValue) {
    this.configValue = configValue;
  }
}

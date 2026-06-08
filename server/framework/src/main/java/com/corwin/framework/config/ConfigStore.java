package com.corwin.framework.config;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置存储抽象。
 *
 * @author Corwin 2026/2/25
 */
public interface ConfigStore {

    List<StoredConfig> findAllActive();

    List<StoredConfig> findByKeyword(String keyword);

    List<StoredConfig> findByLevel(ConfigLevel level);

    Optional<StoredConfig> findByCode(String code);

    boolean updateValue(String code, String value);

}

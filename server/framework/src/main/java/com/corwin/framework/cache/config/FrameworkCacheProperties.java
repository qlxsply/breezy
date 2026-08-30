package com.corwin.framework.cache.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the cache component ({@code framework.cache.*}).
 *
 * @author Corwin 2026/4/19
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "framework.cache")
public class FrameworkCacheProperties {

  private boolean enabled = true;

  private Key key = new Key();

  private Local local = new Local();

  @Getter
  @Setter
  public static class Key {

    private boolean allowEmpty = false;

    private int maxLength = 512;
  }

  @Getter
  @Setter
  public static class Local {

    private Eviction eviction = new Eviction();

    private boolean recordStats = false;

    private Cleanup cleanup = new Cleanup();
  }

  @Getter
  @Setter
  public static class Eviction {

    private boolean enabled = false;

    private long maximumSize = 10_000L;
  }

  @Getter
  @Setter
  public static class Cleanup {

    private boolean enabled = true;

    private Duration interval = Duration.ofSeconds(60);
  }
}

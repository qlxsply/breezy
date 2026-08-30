package com.corwin.framework.config.runtime;

import com.corwin.framework.config.definition.ConfigSpec;
import com.corwin.framework.config.error.MissingConfigException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Corwin 2026/7/30
 */
public final class ConfigRegistry {

  private static final AtomicReference<Map<String, ConfigSnapshot<?>>> CURRENT =
      new AtomicReference<>(Map.of());

  public static void initialize(Map<String, ConfigSnapshot<?>> snapshots) {
    Objects.requireNonNull(snapshots, "snapshots required");
    var copy = new HashMap<String, ConfigSnapshot<?>>();
    snapshots.forEach(
        (key, snapshot) -> {
          Objects.requireNonNull(key, "snapshot key required");
          Objects.requireNonNull(snapshot, "snapshot required");
          if (!key.equals(snapshot.key().value())) {
            throw new IllegalArgumentException(
                "Snapshot map key does not match snapshot key: " + key);
          }
          copy.put(key, snapshot);
        });
    CURRENT.set(Map.copyOf(copy));
  }

  public static <T> T get(ConfigSpec<T> spec) {
    return snapshot(spec).value();
  }

  public static <T> ConfigSnapshot<T> snapshot(ConfigSpec<T> spec) {
    Objects.requireNonNull(spec, "config spec required");
    ConfigSnapshot<?> snapshot = CURRENT.get().get(spec.key().value());
    if (snapshot == null) {
      throw new MissingConfigException(spec.key().value());
    }
    spec.valueClass().cast(snapshot.value());
    return castSnapshot(snapshot);
  }

  public static Map<String, ConfigSnapshot<?>> snapshotAll() {
    return CURRENT.get();
  }

  public static boolean replaceIfNewer(ConfigSnapshot<?> newSnapshot) {
    Objects.requireNonNull(newSnapshot, "newSnapshot required");
    var changed = new AtomicReference<>(false);
    CURRENT.updateAndGet(
        current -> {
          ConfigSnapshot<?> existing = current.get(newSnapshot.key().value());
          if (existing != null && existing.revision() >= newSnapshot.revision()) {
            return current;
          }
          var next = new HashMap<>(current);
          next.put(newSnapshot.key().value(), newSnapshot);
          changed.set(true);
          return Map.copyOf(next);
        });
    return changed.get();
  }

  @SuppressWarnings("unchecked")
  private static <T> ConfigSnapshot<T> castSnapshot(ConfigSnapshot<?> snapshot) {
    return (ConfigSnapshot<T>) snapshot;
  }

  private ConfigRegistry() {}
}

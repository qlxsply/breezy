package com.corwin.jsonfmt.config;

import com.corwin.framework.config.definition.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Corwin 2026/7/31
 */
public final class JsonFmtConfigSpecs {

  public static final ConfigSpec<StoragePolicy> STORAGE_POLICY =
      new SimpleConfigSpec<>(
          new ConfigKey("jsonfmt.storage.policy"),
          "jsonfmt",
          "storage",
          "JSON 内容存储策略",
          "JSON 格式化记录的内容存储策略",
          StoragePolicy.class,
          new StoragePolicy(61_440L),
          1,
          10,
          ConfigActivationPolicy.DYNAMIC,
          ConfigEditPolicy.ADMIN_EDITABLE,
          ConfigInvalidValuePolicy.USE_DEFAULT,
          List.of(
              new ConfigFieldSpec(
                  "contentFileThresholdBytes",
                  "内容转文件阈值",
                  "内容达到该字节数时转为文件存储",
                  ConfigFieldType.LONG,
                  true,
                  false,
                  false,
                  10,
                  "61440",
                  BigDecimal.ONE,
                  null,
                  null,
                  null,
                  List.of())),
          "default",
          JsonFmtConfigSpecs::validateStoragePolicy,
          JsonFmtConfigSpecs::validateStoragePolicy);

  public record StoragePolicy(long contentFileThresholdBytes) {}

  private static List<ConfigViolation> validateStoragePolicy(StoragePolicy value) {
    return value != null && value.contentFileThresholdBytes() > 0
        ? List.of()
        : List.of(
            new ConfigViolation("contentFileThresholdBytes", "OUT_OF_RANGE", "内容转文件阈值必须大于 0"));
  }

  private JsonFmtConfigSpecs() {}
}

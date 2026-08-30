package com.corwin.system.scheduler.infrastructure.scheduling;

import com.corwin.framework.json.Json;
import com.corwin.system.scheduler.domain.model.JobPayload;
import com.corwin.system.scheduler.domain.model.ScheduleRule;
import org.springframework.stereotype.Component;

/**
 * Serializer for serializing and deserializing schedule rules and job payloads to/from JSON bytes.
 *
 * @author Corwin 2026/4/15
 */
@Component
public class SchedulerSerializer {

  /** Serializes a schedule rule to JSON bytes. */
  public byte[] serializeScheduleRule(ScheduleRule scheduleRule) {
    return serialize(scheduleRule);
  }

  /** Deserializes a schedule rule from JSON bytes. */
  public ScheduleRule deserializeScheduleRule(byte[] bytes, String className) {
    return deserialize(bytes, className, ScheduleRule.class);
  }

  /** Serializes a job payload to JSON bytes, returning null if payload is null. */
  public byte[] serializePayload(JobPayload payload) {
    if (payload == null) {
      return null;
    }
    return serialize(payload);
  }

  /**
   * Deserializes a job payload from JSON bytes, returning null if class name or bytes are empty.
   */
  public JobPayload deserializePayload(byte[] bytes, String className) {
    if (className == null || className.isBlank() || bytes == null || bytes.length == 0) {
      return null;
    }
    return deserialize(bytes, className, JobPayload.class);
  }

  private byte[] serialize(Object value) {
    try {
      return Json.toBytes(value);
    } catch (Exception ex) {
      throw new IllegalStateException("scheduler serialize failed", ex);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T deserialize(byte[] bytes, String className, Class<T> expectedType) {
    try {
      Class<?> rawType = Class.forName(className);
      if (!expectedType.isAssignableFrom(rawType)) {
        throw new IllegalStateException("scheduler deserialize type mismatch: " + className);
      }
      return (T) Json.parse(bytes, rawType);
    } catch (Exception ex) {
      throw new IllegalStateException("scheduler deserialize failed: " + className, ex);
    }
  }
}

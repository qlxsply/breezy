package com.corwin.framework.json;

import com.corwin.framework.constant.DateTimePatterns;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * Custom Jackson module that registers application-wide serializers and deserializers.
 *
 * <p>Handles {@link java.time.LocalDateTime}, {@link java.time.LocalDate}, {@link
 * java.time.LocalTime}, {@link java.time.Instant}, and {@link java.util.Date} using the format
 * patterns defined in {@link com.corwin.framework.constant.DateTimePatterns}.
 *
 * @author Corwin 2026/1/7
 */
public class CustomModule extends SimpleModule {

  public CustomModule() {
    super(PackageVersion.VERSION);

    // ======================= 时间序列化规则 ===============================
    this.addSerializer(
        LocalDateTime.class, new LocalDateTimeSerializer(DateTimePatterns.DATE_TIME_FMT));
    this.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimePatterns.DATE_FMT));
    this.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimePatterns.TIME_FMT));
    this.addSerializer(Instant.class, new InstantMillisSerializer());

    // ======================= Date序列化规则 ===============================
    this.addSerializer(Date.class, new DatePatternSerializer(DateTimePatterns.DATE_TIME_PATTERN));

    // ======================= 时间反序列化规则 ==============================
    this.addDeserializer(
        LocalDateTime.class, new LocalDateTimeDeserializer(DateTimePatterns.DATE_TIME_FMT));
    this.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimePatterns.DATE_FMT));
    this.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimePatterns.TIME_FMT));
    this.addDeserializer(Instant.class, new InstantMillisDeserializer());

    // ======================= Date反序列化规则 ===============================
    this.addDeserializer(
        Date.class, new DatePatternDeserializer(DateTimePatterns.DATE_TIME_PATTERN));
  }

  @Override
  public Version version() {
    return super.version();
  }
}

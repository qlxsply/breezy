package com.corwin.system.methodstat.application.service;

import com.corwin.framework.constant.TextConstants;
import com.corwin.framework.domain.page.PageData;
import com.corwin.framework.domain.page.PageSpec;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.error.BizException;
import com.corwin.framework.util.HighDate;
import com.corwin.system.methodstat.application.view.MethodStatStatsView;
import com.corwin.system.methodstat.domain.model.*;
import com.corwin.system.methodstat.domain.repo.MethodStatAggregateRepository;
import com.corwin.system.methodstat.domain.repo.MethodStatMetadataRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Application service for querying method invocation statistics with pagination, filtering, and
 * sorting.
 *
 * @author Corwin 2026/3/25
 */
@Service
@RequiredArgsConstructor
public class MethodStatQueryAppService {

  private static final String SORT_DIRECTION_DESC = "DESC";
  private static final String SORT_BY_METHOD_NAME = "METHOD_NAME";

  private final MethodStatMetadataRepository metadataRepository;
  private final MethodStatAggregateRepository aggregateRepository;
  private final MethodStatSwitchAppService switchAppService;
  private final MethodStatMetadataCollector metadataCollector;

  /**
   * Retrieve a paginated list of method statistics, filtered by method name and sorted by the given
   * field.
   *
   * @param methodName optional method keyword filter
   * @param collectEnabled optional effective collection status filter
   * @param sortBy field to sort by
   * @param sortDirection sort direction (ASC or DESC)
   * @param pageNo page number (1-based)
   * @param pageSize page size
   * @return paginated method statistics
   */
  public PageData<MethodStatStatsView> pageStats(
      String methodName,
      Boolean collectEnabled,
      String sortBy,
      String sortDirection,
      Integer pageNo,
      Integer pageSize) {
    long nowMillis = HighDate.realTimestampMillis();
    List<MethodStatMetadata> metadata = metadataRepository.findAll();
    if (metadata.isEmpty()) {
      metadataCollector.collectAllPointcutMetadata();
      metadata = metadataRepository.findAll();
    }
    List<MethodStatStatsView> all =
        metadata.stream()
            .filter(item -> matchesMethod(item, methodName))
            .map(item -> toStatsView(item, nowMillis))
            .filter(
                item ->
                    collectEnabled == null
                        || item.collectEnabled() == collectEnabled.booleanValue())
            .sorted(buildStatsComparator(sortBy, sortDirection))
            .toList();
    return page(all, pageNo, pageSize);
  }

  /**
   * Retrieve detailed statistics for a specific method by its key.
   *
   * @param key the method key string
   * @return the method statistics view
   */
  @MethodStat
  public MethodStatStatsView getMethodStats(String key) {
    BizAssert.notBlank(key, BaseError.MISSING_PARAMETER);
    MethodStatKey methodStatKey = MethodStatKey.of(key.trim());
    MethodStatMetadata metadata =
        metadataRepository
            .findByKey(methodStatKey)
            .orElseThrow(
                () ->
                    new BizException(
                        "Method metadata not found: " + methodStatKey.value(),
                        BaseError.NOT_FOUND));
    return toStatsView(metadata, HighDate.realTimestampMillis());
  }

  /**
   * Convert metadata and its aggregate snapshot into a complete stats view.
   *
   * @param metadata the method metadata
   * @param nowMillis current timestamp for window calculations
   * @return the stats view
   */
  private MethodStatStatsView toStatsView(MethodStatMetadata metadata, long nowMillis) {
    MethodStatAggregateSnapshot snapshot =
        aggregateRepository
            .findByKey(metadata.key())
            .map(item -> item.snapshot(nowMillis))
            .orElseGet(() -> emptySnapshot(metadata.key()));

    MethodStatDurationMetrics metrics = snapshot.durationMetrics();
    boolean globalSwitchEnabled = switchAppService.isGlobalEnabled();
    boolean methodSwitchEnabled = switchAppService.isMethodEnabled(metadata.key());

    return new MethodStatStatsView(
        metadata.key().value(),
        metadata.packageName(),
        metadata.className(),
        metadata.methodName(),
        metadata.methodSignature(),
        methodSwitchEnabled,
        globalSwitchEnabled,
        globalSwitchEnabled && methodSwitchEnabled,
        snapshot.totalCalls(),
        snapshot.totalSuccess(),
        snapshot.totalFailure(),
        snapshot.recent1MinuteCalls(),
        snapshot.recent1HourCalls(),
        snapshot.recent1DayCalls(),
        snapshot.recent1MinuteSuccess(),
        snapshot.recent1MinuteFailure(),
        snapshot.recent1HourSuccess(),
        snapshot.recent1HourFailure(),
        snapshot.recent1DaySuccess(),
        snapshot.recent1DayFailure(),
        metrics.sampleSize(),
        metrics.max(),
        metrics.min(),
        metrics.avg(),
        metrics.p50(),
        metrics.p90(),
        metrics.p95(),
        metrics.p99());
  }

  /**
   * Create an empty snapshot with zero values for the given key.
   *
   * @param key the method key
   * @return empty snapshot
   */
  private MethodStatAggregateSnapshot emptySnapshot(MethodStatKey key) {
    return new MethodStatAggregateSnapshot(
        key, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, MethodStatDurationMetrics.empty());
  }

  /**
   * Check whether any method identity field contains the keyword, ignoring case.
   *
   * @param metadata method metadata
   * @param methodName optional method keyword
   * @return true if the method name matches the filter
   */
  private boolean matchesMethod(MethodStatMetadata metadata, String methodName) {
    String normalizedFilter = normalize(methodName);
    if (normalizedFilter == null) {
      return true;
    }
    String keyword = normalizedFilter.toLowerCase(Locale.ROOT);
    return containsIgnoreCase(metadata.packageName(), keyword)
        || containsIgnoreCase(metadata.className(), keyword)
        || containsIgnoreCase(metadata.methodName(), keyword)
        || containsIgnoreCase(metadata.methodSignature(), keyword)
        || containsIgnoreCase(metadata.key().value(), keyword);
  }

  private boolean containsIgnoreCase(String value, String lowercaseKeyword) {
    String current = value == null ? TextConstants.EMPTY : value;
    return current.toLowerCase(Locale.ROOT).contains(lowercaseKeyword);
  }

  /**
   * Build a comparator for method stats views based on the specified sort field and direction.
   *
   * @param sortBy the field to sort by
   * @param sortDirection sort direction (ASC or DESC)
   * @return configured comparator
   */
  private Comparator<MethodStatStatsView> buildStatsComparator(
      String sortBy, String sortDirection) {
    String normalizedSortBy = normalize(sortBy);
    if (normalizedSortBy == null) {
      normalizedSortBy = SORT_BY_METHOD_NAME;
    }
    Comparator<MethodStatStatsView> comparator =
        switch (normalizedSortBy.toUpperCase(Locale.ROOT)) {
          case "TOTAL_SUCCESS" -> Comparator.comparingLong(MethodStatStatsView::totalSuccess);
          case "TOTAL_FAILURE" -> Comparator.comparingLong(MethodStatStatsView::totalFailure);
          case "RECENT_1M_CALLS" ->
              Comparator.comparingLong(MethodStatStatsView::recent1MinuteCalls);
          case "RECENT_1H_CALLS" -> Comparator.comparingLong(MethodStatStatsView::recent1HourCalls);
          case "RECENT_1D_CALLS" -> Comparator.comparingLong(MethodStatStatsView::recent1DayCalls);
          case "RECENT_1M_SUCCESS" ->
              Comparator.comparingLong(MethodStatStatsView::recent1MinuteSuccess);
          case "RECENT_1M_FAILURE" ->
              Comparator.comparingLong(MethodStatStatsView::recent1MinuteFailure);
          case "RECENT_1H_SUCCESS" ->
              Comparator.comparingLong(MethodStatStatsView::recent1HourSuccess);
          case "RECENT_1H_FAILURE" ->
              Comparator.comparingLong(MethodStatStatsView::recent1HourFailure);
          case "RECENT_1D_SUCCESS" ->
              Comparator.comparingLong(MethodStatStatsView::recent1DaySuccess);
          case "RECENT_1D_FAILURE" ->
              Comparator.comparingLong(MethodStatStatsView::recent1DayFailure);
          case "DURATION_MAX" -> Comparator.comparingLong(MethodStatStatsView::durationMax);
          case "DURATION_MIN" -> Comparator.comparingLong(MethodStatStatsView::durationMin);
          case "DURATION_AVG" -> Comparator.comparingDouble(MethodStatStatsView::durationAvg);
          case "DURATION_P50" -> Comparator.comparingLong(MethodStatStatsView::durationP50);
          case "DURATION_P90" -> Comparator.comparingLong(MethodStatStatsView::durationP90);
          case "DURATION_P95" -> Comparator.comparingLong(MethodStatStatsView::durationP95);
          case "DURATION_P99" -> Comparator.comparingLong(MethodStatStatsView::durationP99);
          case "METHOD_NAME" ->
              Comparator.comparing(
                  MethodStatStatsView::methodName,
                  Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
          case "KEY" -> Comparator.comparing(MethodStatStatsView::key);
          default -> Comparator.comparingLong(MethodStatStatsView::totalCalls);
        };

    String normalizedSortDirection = normalize(sortDirection);
    if (SORT_DIRECTION_DESC.equalsIgnoreCase(normalizedSortDirection)) {
      comparator = comparator.reversed();
    }
    return comparator.thenComparing(MethodStatStatsView::key);
  }

  /**
   * Normalize a string value by trimming it; returns null if blank or null.
   *
   * @param value the input value
   * @return normalized value or null
   */
  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    return normalized;
  }

  /**
   * Apply in-memory pagination on a list.
   *
   * @param all the full list of items
   * @param pageNo page number (1-based)
   * @param pageSize items per page
   * @return paginated result
   */
  private <T> PageData<T> page(List<T> all, Integer pageNo, Integer pageSize) {
    PageSpec pageSpec = PageSpec.of(pageNo, pageSize, List.of());
    int from = (pageSpec.pageNo() - 1) * pageSpec.pageSize();
    if (from >= all.size()) {
      return PageData.of(pageSpec.pageNo(), pageSpec.pageSize(), all.size(), List.of());
    }
    int to = Math.min(from + pageSpec.pageSize(), all.size());
    return PageData.of(pageSpec.pageNo(), pageSpec.pageSize(), all.size(), all.subList(from, to));
  }
}

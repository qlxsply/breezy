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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * @author Corwin 2026/3/25
 */
@Service
@RequiredArgsConstructor
public class MethodStatQueryAppService {

    private static final String MATCH_MODE_EXACT = "EXACT";
    private static final String SORT_DIRECTION_DESC = "DESC";
    private static final String SORT_BY_METHOD_NAME = "METHOD_NAME";

    private final MethodStatMetadataRepository metadataRepository;
    private final MethodStatAggregateRepository aggregateRepository;
    private final MethodStatSwitchAppService switchAppService;

    public PageData<MethodStatStatsView> pageStats(String methodName, String matchMode, String sortBy,
            String sortDirection, Integer pageNo, Integer pageSize) {
        long nowMillis = HighDate.realTimestampMillis();
        List<MethodStatStatsView> all = metadataRepository.findAll().stream()
                .filter(item -> matchesMethodName(item.methodName(), methodName, matchMode))
                .map(item -> toStatsView(item, nowMillis)).sorted(buildStatsComparator(sortBy, sortDirection)).toList();
        return page(all, pageNo, pageSize);
    }

    @MethodStat
    public MethodStatStatsView getMethodStats(String key) {
        BizAssert.notBlank(key, BaseError.MISSING_PARAMETER);
        MethodStatKey methodStatKey = MethodStatKey.of(key.trim());
        MethodStatMetadata metadata = metadataRepository.findByKey(methodStatKey).orElseThrow(
                () -> new BizException("Method metadata not found: " + methodStatKey.value(), BaseError.NOT_FOUND));
        return toStatsView(metadata, HighDate.realTimestampMillis());
    }

    private MethodStatStatsView toStatsView(MethodStatMetadata metadata, long nowMillis) {
        MethodStatAggregateSnapshot snapshot = aggregateRepository.findByKey(metadata.key())
                .map(item -> item.snapshot(nowMillis)).orElseGet(() -> emptySnapshot(metadata.key()));

        MethodStatDurationMetrics metrics = snapshot.durationMetrics();
        boolean globalSwitchEnabled = switchAppService.isGlobalEnabled();
        boolean methodSwitchEnabled = switchAppService.isMethodEnabled(metadata.key());

        return new MethodStatStatsView(metadata.key().value(), metadata.packageName(), metadata.className(),
                metadata.methodName(), metadata.methodSignature(), methodSwitchEnabled, globalSwitchEnabled,
                globalSwitchEnabled && methodSwitchEnabled, snapshot.totalCalls(), snapshot.totalSuccess(),
                snapshot.totalFailure(), snapshot.recent1MinuteCalls(), snapshot.recent1HourCalls(),
                snapshot.recent1DayCalls(), snapshot.recent1MinuteSuccess(), snapshot.recent1MinuteFailure(),
                snapshot.recent1HourSuccess(), snapshot.recent1HourFailure(), snapshot.recent1DaySuccess(),
                snapshot.recent1DayFailure(), metrics.sampleSize(), metrics.max(), metrics.min(), metrics.avg(),
                metrics.p50(), metrics.p90(), metrics.p95(), metrics.p99());
    }

    private MethodStatAggregateSnapshot emptySnapshot(MethodStatKey key) {
        return new MethodStatAggregateSnapshot(key, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                MethodStatDurationMetrics.empty());
    }

    private boolean matchesMethodName(String currentMethodName, String methodName, String matchMode) {
        String normalizedFilter = normalize(methodName);
        if (normalizedFilter == null) {
            return true;
        }
        String current = currentMethodName == null ? TextConstants.EMPTY : currentMethodName;
        if (MATCH_MODE_EXACT.equalsIgnoreCase(normalize(matchMode))) {
            return current.equalsIgnoreCase(normalizedFilter);
        }
        return current.toLowerCase(Locale.ROOT).contains(normalizedFilter.toLowerCase(Locale.ROOT));
    }

    private Comparator<MethodStatStatsView> buildStatsComparator(String sortBy, String sortDirection) {
        String normalizedSortBy = normalize(sortBy);
        if (normalizedSortBy == null) {
            normalizedSortBy = SORT_BY_METHOD_NAME;
        }
        Comparator<MethodStatStatsView> comparator = switch (normalizedSortBy.toUpperCase(Locale.ROOT)) {
            case "TOTAL_SUCCESS" -> Comparator.comparingLong(MethodStatStatsView::totalSuccess);
            case "TOTAL_FAILURE" -> Comparator.comparingLong(MethodStatStatsView::totalFailure);
            case "RECENT_1M_CALLS" -> Comparator.comparingLong(MethodStatStatsView::recent1MinuteCalls);
            case "RECENT_1H_CALLS" -> Comparator.comparingLong(MethodStatStatsView::recent1HourCalls);
            case "RECENT_1D_CALLS" -> Comparator.comparingLong(MethodStatStatsView::recent1DayCalls);
            case "RECENT_1M_SUCCESS" -> Comparator.comparingLong(MethodStatStatsView::recent1MinuteSuccess);
            case "RECENT_1M_FAILURE" -> Comparator.comparingLong(MethodStatStatsView::recent1MinuteFailure);
            case "RECENT_1H_SUCCESS" -> Comparator.comparingLong(MethodStatStatsView::recent1HourSuccess);
            case "RECENT_1H_FAILURE" -> Comparator.comparingLong(MethodStatStatsView::recent1HourFailure);
            case "RECENT_1D_SUCCESS" -> Comparator.comparingLong(MethodStatStatsView::recent1DaySuccess);
            case "RECENT_1D_FAILURE" -> Comparator.comparingLong(MethodStatStatsView::recent1DayFailure);
            case "DURATION_MAX" -> Comparator.comparingLong(MethodStatStatsView::durationMax);
            case "DURATION_MIN" -> Comparator.comparingLong(MethodStatStatsView::durationMin);
            case "DURATION_AVG" -> Comparator.comparingDouble(MethodStatStatsView::durationAvg);
            case "DURATION_P50" -> Comparator.comparingLong(MethodStatStatsView::durationP50);
            case "DURATION_P90" -> Comparator.comparingLong(MethodStatStatsView::durationP90);
            case "DURATION_P95" -> Comparator.comparingLong(MethodStatStatsView::durationP95);
            case "DURATION_P99" -> Comparator.comparingLong(MethodStatStatsView::durationP99);
            case "METHOD_NAME" -> Comparator.comparing(MethodStatStatsView::methodName,
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

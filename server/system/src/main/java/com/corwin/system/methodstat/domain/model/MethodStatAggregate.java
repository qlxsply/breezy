package com.corwin.system.methodstat.domain.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Domain aggregate that maintains sliding-window invocation counts and duration samples for a
 * single monitored method. Uses time-windowed bucket counters for efficient recent-call tracking
 * (last minute, hour, day).
 *
 * @author Corwin 2026/3/25
 */
public class MethodStatAggregate {

  private static final int SECOND_BUCKET_SIZE = 60;
  private static final int MINUTE_BUCKET_SIZE = 60;
  private static final int HOUR_BUCKET_SIZE = 24;
  private static final int DURATION_SAMPLE_SIZE = 100;

  private final MethodStatKey key;
  private final WindowBucket[] secondBuckets = createBuckets(SECOND_BUCKET_SIZE);
  private final WindowBucket[] minuteBuckets = createBuckets(MINUTE_BUCKET_SIZE);
  private final WindowBucket[] hourBuckets = createBuckets(HOUR_BUCKET_SIZE);
  private final long[] durationSamples = new long[DURATION_SAMPLE_SIZE];

  private long totalCalls;
  private long totalSuccess;
  private long totalFailure;

  private int durationSampleCount;
  private int durationSampleCursor;

  /**
   * Create a new aggregate for the given method key.
   *
   * @param key the unique method identifier
   */
  public MethodStatAggregate(MethodStatKey key) {
    this.key = Objects.requireNonNull(key, "key required");
  }

  /**
   * @return the unique method key for this aggregate
   */
  public MethodStatKey key() {
    return key;
  }

  /**
   * Apply an invocation event to this aggregate, updating all counters and duration samples.
   *
   * @param event the invocation event to process
   */
  public synchronized void apply(MethodStatInvocationEvent event) {
    Objects.requireNonNull(event, "event required");

    totalCalls++;
    if (event.success()) {
      totalSuccess++;
    } else {
      totalFailure++;
    }

    long epochSecond = Math.floorDiv(event.finishedAtMillis(), 1000L);
    updateSecondBucket(epochSecond, event.success());
    updateMinuteBucket(epochSecond, event.success());
    updateHourBucket(epochSecond, event.success());
    appendDuration(event.durationMillis());
  }

  /**
   * Take a point-in-time snapshot of all aggregated statistics.
   *
   * @param nowMillis current timestamp in milliseconds for window computations
   * @return the aggregate snapshot
   */
  public synchronized MethodStatAggregateSnapshot snapshot(long nowMillis) {
    long epochSecond = Math.floorDiv(nowMillis, 1000L);

    BucketCount secondWindow = sumWindow(secondBuckets, epochSecond - 59L, epochSecond);
    long epochMinute = Math.floorDiv(epochSecond, 60L);
    BucketCount minuteWindow = sumWindow(minuteBuckets, epochMinute - 59L, epochMinute);
    long epochHour = Math.floorDiv(epochSecond, 3600L);
    BucketCount hourWindow = sumWindow(hourBuckets, epochHour - 23L, epochHour);

    return new MethodStatAggregateSnapshot(
        key,
        totalCalls,
        totalSuccess,
        totalFailure,
        secondWindow.total(),
        minuteWindow.total(),
        hourWindow.total(),
        secondWindow.success(),
        secondWindow.failure(),
        minuteWindow.success(),
        minuteWindow.failure(),
        hourWindow.success(),
        hourWindow.failure(),
        calculateDurationMetrics());
  }

  /** Reset all counters and samples to their initial state. */
  public synchronized void clear() {
    totalCalls = 0L;
    totalSuccess = 0L;
    totalFailure = 0L;
    clearBuckets(secondBuckets);
    clearBuckets(minuteBuckets);
    clearBuckets(hourBuckets);
    Arrays.fill(durationSamples, 0L);
    durationSampleCount = 0;
    durationSampleCursor = 0;
  }

  private void updateSecondBucket(long epochSecond, boolean success) {
    int index = Math.floorMod(epochSecond, SECOND_BUCKET_SIZE);
    secondBuckets[index].add(epochSecond, success);
  }

  private void updateMinuteBucket(long epochSecond, boolean success) {
    long epochMinute = Math.floorDiv(epochSecond, 60L);
    int index = Math.floorMod(epochMinute, MINUTE_BUCKET_SIZE);
    minuteBuckets[index].add(epochMinute, success);
  }

  private void updateHourBucket(long epochSecond, boolean success) {
    long epochHour = Math.floorDiv(epochSecond, 3600L);
    int index = Math.floorMod(epochHour, HOUR_BUCKET_SIZE);
    hourBuckets[index].add(epochHour, success);
  }

  private void appendDuration(long durationMillis) {
    durationSamples[durationSampleCursor] = Math.max(0L, durationMillis);
    durationSampleCursor = (durationSampleCursor + 1) % DURATION_SAMPLE_SIZE;
    if (durationSampleCount < DURATION_SAMPLE_SIZE) {
      durationSampleCount++;
    }
  }

  private MethodStatDurationMetrics calculateDurationMetrics() {
    if (durationSampleCount == 0) {
      return MethodStatDurationMetrics.empty();
    }

    long[] samples = Arrays.copyOf(durationSamples, durationSampleCount);
    Arrays.sort(samples);

    long min = samples[0];
    long max = samples[samples.length - 1];

    double sum = 0D;
    for (long sample : samples) {
      sum += sample;
    }

    return new MethodStatDurationMetrics(
        durationSampleCount,
        min,
        max,
        sum / durationSampleCount,
        percentile(samples, 50),
        percentile(samples, 90),
        percentile(samples, 95),
        percentile(samples, 99));
  }

  private long percentile(long[] sortedSamples, int percentile) {
    if (sortedSamples.length == 0) {
      return 0L;
    }
    int rank = (int) Math.ceil(percentile / 100D * sortedSamples.length);
    int index = Math.max(0, rank - 1);
    return sortedSamples[index];
  }

  private BucketCount sumWindow(
      WindowBucket[] buckets, long minSlotInclusive, long maxSlotInclusive) {
    long total = 0L;
    long success = 0L;
    long failure = 0L;
    for (WindowBucket bucket : buckets) {
      if (!bucket.inRange(minSlotInclusive, maxSlotInclusive)) {
        continue;
      }
      total += bucket.total();
      success += bucket.success();
      failure += bucket.failure();
    }
    return new BucketCount(total, success, failure);
  }

  private WindowBucket[] createBuckets(int size) {
    WindowBucket[] buckets = new WindowBucket[size];
    for (int i = 0; i < size; i++) {
      buckets[i] = new WindowBucket();
    }
    return buckets;
  }

  private void clearBuckets(WindowBucket[] buckets) {
    for (WindowBucket bucket : buckets) {
      bucket.clear();
    }
  }

  private static final class WindowBucket {

    private long slot = Long.MIN_VALUE;
    private long total;
    private long success;
    private long failure;

    private void add(long nextSlot, boolean successFlag) {
      if (slot != nextSlot) {
        slot = nextSlot;
        total = 0L;
        success = 0L;
        failure = 0L;
      }
      total++;
      if (successFlag) {
        success++;
      } else {
        failure++;
      }
    }

    private boolean inRange(long minSlotInclusive, long maxSlotInclusive) {
      return slot >= minSlotInclusive && slot <= maxSlotInclusive;
    }

    private long total() {
      return total;
    }

    private long success() {
      return success;
    }

    private long failure() {
      return failure;
    }

    private void clear() {
      slot = Long.MIN_VALUE;
      total = 0L;
      success = 0L;
      failure = 0L;
    }
  }

  private record BucketCount(long total, long success, long failure) {}
}

package com.corwin.system.methodstat.application.view;

/**
 * View object representing aggregated method invocation statistics, including call counts,
 * success/failure rates across recent time windows, and duration percentiles.
 *
 * @author Corwin 2026/3/25
 */
public record MethodStatStatsView(
    String key,
    String packageName,
    String className,
    String methodName,
    String methodSignature,
    boolean methodSwitchEnabled,
    boolean globalSwitchEnabled,
    boolean collectEnabled,
    long totalCalls,
    long totalSuccess,
    long totalFailure,
    long recent1MinuteCalls,
    long recent1HourCalls,
    long recent1DayCalls,
    long recent1MinuteSuccess,
    long recent1MinuteFailure,
    long recent1HourSuccess,
    long recent1HourFailure,
    long recent1DaySuccess,
    long recent1DayFailure,
    int durationSampleSize,
    long durationMax,
    long durationMin,
    double durationAvg,
    long durationP50,
    long durationP90,
    long durationP95,
    long durationP99) {}

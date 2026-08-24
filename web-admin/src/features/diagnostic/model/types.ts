export type DiagnosticItem = "JVM" | "OS" | "THREAD" | "HTTP" | "DB_POOL" | "SQL" | "JFR";

export type DiagnosticStatus = "INACTIVE" | "ACTIVE";

export type DiagnosticEventType =
  | "HTTP_SLOW_REQUEST"
  | "HTTP_ERROR_REQUEST"
  | "SQL_SLOW"
  | "SQL_ERROR"
  | "JFR_GC"
  | "JFR_EXCEPTION"
  | "JFR_THREAD_PARK"
  | "JFR_MONITOR_BLOCKED";

export interface DiagnosticConfig {
  intervalMs: number;
  historyCapacity: number;
  eventCapacity: number;
  items: DiagnosticItem[];
  deepMode: boolean;
  slowRequestThresholdMs: number;
  slowSqlThresholdMs: number;
  ttlSeconds: number;
}

export interface DiagnosticSession {
  status: DiagnosticStatus;
  config: DiagnosticConfig;
  startedAt?: string | null;
  expireAt?: string | null;
  remainingTtlSeconds: number;
}

export interface DiagnosticCapability {
  jfrAvailable: boolean;
  httpAvailable: boolean;
  dbPoolAvailable: boolean;
  sqlAvailable: boolean;
  deepModeSupported: boolean;
  dataSourceNames: string[];
}

export interface MemoryPoolSnapshot {
  name: string;
  usedBytes: number;
  committedBytes: number;
  maxBytes: number;
}

export interface JvmSnapshot {
  startedAt?: string | null;
  uptimeMs: number;
  javaVersion: string;
  inputArguments: string[];
  heapUsedBytes: number;
  heapCommittedBytes: number;
  heapMaxBytes: number;
  nonHeapUsedBytes: number;
  nonHeapCommittedBytes: number;
  nonHeapMaxBytes: number;
  memoryPools: MemoryPoolSnapshot[];
  loadedClassCount: number;
  totalLoadedClassCount: number;
  unloadedClassCount: number;
  processCpuLoad: number;
  processCpuTimeMs: number;
  gcCollectionCount: number;
  gcCollectionTimeMs: number;
}

export interface NetworkInterfaceSnapshot {
  name: string;
  displayName: string;
  up: boolean;
  loopback: boolean;
  addresses: string[];
}

export interface DiskSnapshot {
  name: string;
  type: string;
  totalBytes: number;
  usableBytes: number;
  unallocatedBytes: number;
}

export interface OsSnapshot {
  hostName: string;
  ipAddresses: string[];
  networkInterfaces: NetworkInterfaceSnapshot[];
  osName: string;
  osVersion: string;
  osArch: string;
  availableProcessors: number;
  systemCpuLoad: number;
  totalPhysicalMemoryBytes: number;
  freePhysicalMemoryBytes: number;
  totalSwapSpaceBytes: number;
  freeSwapSpaceBytes: number;
  disks: DiskSnapshot[];
}

export interface ThreadSnapshot {
  threadCount: number;
  daemonThreadCount: number;
  peakThreadCount: number;
  totalStartedThreadCount: number;
  runnableCount: number;
  blockedCount: number;
  waitingCount: number;
  timedWaitingCount: number;
  deadlockedThreadIds: number[];
}

export interface HttpUriStatSnapshot {
  method: string;
  uri: string;
  totalRequests: number;
  totalDurationMs: number;
  averageDurationMs: number;
  slowRequestCount: number;
  errorRequestCount: number;
}

export interface HttpSnapshot {
  inFlightRequests: number;
  totalRequests: number;
  averageDurationMs: number;
  slowRequestCount: number;
  errorRequestCount: number;
  statusCounts: Record<string, number>;
  p95DurationMs: number;
  p99DurationMs: number;
  topUris: HttpUriStatSnapshot[];
}

export interface DbPoolEntrySnapshot {
  beanName: string;
  poolName: string;
  activeConnections: number;
  idleConnections: number;
  totalConnections: number;
  waitingThreads: number;
}

export interface DbPoolSnapshot {
  poolCount: number;
  activeConnections: number;
  idleConnections: number;
  totalConnections: number;
  waitingThreads: number;
  pools: DbPoolEntrySnapshot[];
}

export interface SqlStatementStatSnapshot {
  dataSourceName: string;
  sql: string;
  totalExecutions: number;
  totalDurationMs: number;
  averageDurationMs: number;
  slowExecutions: number;
  errorExecutions: number;
  maxDurationMs: number;
}

export interface SqlSnapshot {
  totalExecutions: number;
  averageDurationMs: number;
  slowSqlCount: number;
  errorCount: number;
  topStatements: SqlStatementStatSnapshot[];
}

export interface JfrSnapshot {
  running: boolean;
  gcEventCount: number;
  gcPauseTimeMs: number;
  exceptionEventCount: number;
  threadParkEventCount: number;
  monitorBlockedEventCount: number;
}

export interface DiagnosticSnapshot {
  capturedAt: string;
  jvm?: JvmSnapshot | null;
  os?: OsSnapshot | null;
  thread?: ThreadSnapshot | null;
  http?: HttpSnapshot | null;
  dbPool?: DbPoolSnapshot | null;
  sql?: SqlSnapshot | null;
  jfr?: JfrSnapshot | null;
}

export interface DiagnosticEvent {
  id: string;
  type: DiagnosticEventType;
  happenedAt: string;
  title: string;
  message: string;
  details: Record<string, unknown>;
}

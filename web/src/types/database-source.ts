// /src/types/database-source.ts

import type { PageQuery } from "./page";

export type DatabaseType = "H2" | "MYSQL" | "POSTGRESQL" | "ORACLE" | "SQLSERVER";
export type AuthMode = "PASSWORD";
export type ConnectMode = "HOST_PORT" | "SERVICE_NAME" | "SID" | "TNS";
export type DatabaseSourceStatus = "NEW" | "OK" | "FAILED";
export type DatabaseSourceSourceType = "APP" | "USER";

export interface DatabaseSource {
  id: string;
  name: string;
  dbType: DatabaseType;
  authMode?: AuthMode;
  connectMode?: ConnectMode;
  host?: string | null;
  port?: number | null;
  databaseName?: string | null;
  serviceName?: string | null;
  sid?: string | null;
  driverClassName?: string | null;
  extraParams?: string | null;
  jdbcUrl: string;
  username: string;
  remarkCustom?: string;
  sourceType: DatabaseSourceSourceType;
  status: DatabaseSourceStatus;
  lastTestTime?: string;
  lastOkTime?: string;
  lastError?: string;
}

export interface DatabaseSourceSimple {
  id: string;
  name: string;
  dbType: DatabaseType;
  username: string;
  status: DatabaseSourceStatus;
  sourceType: DatabaseSourceSourceType;
}

export interface DatabaseSourceConnectionInfo {
  id: string;
  name: string;
  dbType: DatabaseType;
  authMode?: AuthMode;
  connectMode?: ConnectMode;
  host?: string | null;
  port?: number | null;
  databaseName?: string | null;
  serviceName?: string | null;
  sid?: string | null;
  driverClassName?: string | null;
  extraParams?: string | null;
  jdbcUrl: string;
  username: string;
  passwordRaw?: string | null;
  remarkCustom?: string | null;
}

export interface DatabaseSchema {
  id: string;
  dataSourceId: string | null;
  databaseName: string;
  alias?: string;
  remarkCustom?: string;
  productName?: string;
  productVersion?: string;
  driverName?: string;
  driverVersion?: string;
  fetchedAt?: string;
  createdAt?: string;
}

export interface DatabaseSchemaBasicInfo {
  dataSourceName: string;
  dbType: DatabaseType;
  jdbcUrl: string;
  username: string;
  passwordRaw?: string | null;
}

export interface DatabaseTable {
  id: string;
  databaseId: string;
  tableCatalog?: string;
  tableSchema?: string;
  tableName: string;
  tableType?: string;
  remarkDb?: string;
  remarkCustom?: string;
  alias?: string;
}

export interface DatabaseColumn {
  id: string;
  databaseId: string;
  tableId: string;
  columnName: string;
  typeName?: string;
  jdbcType?: number;
  columnSize?: number;
  decimalDigits?: number;
  nullable?: boolean;
  ordinalPosition?: number;
  defaultValue?: string;
  remarkDb?: string;
  remarkCustom?: string;
  alias?: string;
}

export interface DatabaseSourcePageRequest extends PageQuery {
  nameLike?: string;
  dbType?: DatabaseType;
}

export interface UpsertDatabaseSourceRequest {
  id?: string | null;
  name: string;
  dbType: DatabaseType;
  authMode?: AuthMode;
  connectMode?: ConnectMode;
  host?: string | null;
  port?: number | null;
  databaseName?: string | null;
  serviceName?: string | null;
  sid?: string | null;
  driverClassName?: string | null;
  extraParams?: string | null;
  jdbcUrl: string;
  username: string;
  passwordRaw?: string;
  remarkCustom?: string;
}

export interface TestConnectionRes {
  success: boolean;
  message?: string | null;
}

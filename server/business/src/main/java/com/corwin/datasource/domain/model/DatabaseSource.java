package com.corwin.datasource.domain.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

/**
 *
 * @author Corwin 2026/1/11
 */
@Getter
@Entity
@Table(name = "db_source", indexes = {@Index(name = "idx_db_source_name", columnList = "name"),
        @Index(name = "idx_db_source_type", columnList = "db_type"),
        @Index(name = "idx_db_source_status", columnList = "ds_status")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_db_source_name", columnNames = {"name"})})
public class DatabaseSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 数据源名称
     */
    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false, length = 32)
    private DatabaseType dbType;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_mode", nullable = false, length = 32)
    private AuthMode authMode = AuthMode.PASSWORD;

    @Enumerated(EnumType.STRING)
    @Column(name = "connect_mode", length = 32)
    private ConnectMode connectMode = ConnectMode.HOST_PORT;

    @Column(length = 128)
    private String host;

    @Column
    private Integer port;

    @Column(name = "database_name", length = 128)
    private String databaseName;

    @Column(name = "service_name", length = 128)
    private String serviceName;

    @Column(name = "sid", length = 128)
    private String sid;

    @Column(name = "driver_class", length = 256)
    private String driverClassName;

    /**
     * JDBC URL
     */
    @Column(name = "jdbc_url", nullable = false, length = 512)
    private String jdbcUrl;

    @Column(nullable = false, length = 128)
    private String username;

    /**
     * 密码
     */
    @Column(name = "password_enc", length = 512)
    private String passwordEnc;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private DatabaseSourceSourceType sourceType = DatabaseSourceSourceType.USER;

    @Column(name = "app_ds_key", length = 128)
    private String appDsKey;

    /**
     * 备注
     */
    @Column(name = "remark_custom", length = 512)
    private String remarkCustom;

    @Column(name = "extra_params", length = 2000)
    private String extraParams;

    @Enumerated(EnumType.STRING)
    @Column(name = "ds_status", nullable = false, length = 16)
    private DatabaseSourceStatus status = DatabaseSourceStatus.NEW;

    @Column(name = "last_test_time")
    private Instant lastTestTime;

    @Column(name = "last_ok_time")
    private Instant lastOkTime;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected DatabaseSource() {
    }

    public static DatabaseSource createUser(String name, DatabaseType dbType, String jdbcUrl, String username,
            String passwordEnc, String remarkCustom, AuthMode authMode, ConnectMode connectMode, String host,
            Integer port, String databaseName, String serviceName, String sid, String driverClassName,
            String extraParams) {
        DatabaseSource c = new DatabaseSource();
        c.name = name;
        c.dbType = dbType;
        c.jdbcUrl = jdbcUrl;
        c.username = username;
        c.passwordEnc = passwordEnc;
        c.remarkCustom = remarkCustom;
        c.authMode = authMode;
        c.connectMode = connectMode;
        c.host = host;
        c.port = port;
        c.databaseName = databaseName;
        c.serviceName = serviceName;
        c.sid = sid;
        c.driverClassName = driverClassName;
        c.extraParams = extraParams;
        c.sourceType = DatabaseSourceSourceType.USER;
        c.appDsKey = null;
        return c;
    }

    public static DatabaseSource createApp(String name, DatabaseType dbType, String jdbcUrl, String username,
            String appDsKey, String remarkCustom) {
        DatabaseSource c = new DatabaseSource();
        c.name = name;
        c.dbType = dbType;
        c.jdbcUrl = jdbcUrl;
        c.username = username;
        c.passwordEnc = null;
        c.remarkCustom = remarkCustom;
        c.authMode = AuthMode.PASSWORD;
        c.connectMode = ConnectMode.HOST_PORT;
        c.host = null;
        c.port = null;
        c.databaseName = null;
        c.serviceName = null;
        c.sid = null;
        c.driverClassName = null;
        c.extraParams = null;
        c.sourceType = DatabaseSourceSourceType.APP;
        c.appDsKey = appDsKey;
        return c;
    }

    public void updateUserConfig(String name, DatabaseType dbType, String jdbcUrl, String username, String passwordEnc,
            String remarkCustom, AuthMode authMode, ConnectMode connectMode, String host, Integer port,
            String databaseName, String serviceName, String sid, String driverClassName, String extraParams) {
        this.name = name;
        this.dbType = dbType;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.passwordEnc = passwordEnc;
        this.remarkCustom = remarkCustom;
        this.authMode = authMode;
        this.connectMode = connectMode;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.serviceName = serviceName;
        this.sid = sid;
        this.driverClassName = driverClassName;
        this.extraParams = extraParams;
        this.sourceType = DatabaseSourceSourceType.USER;
        this.appDsKey = null;
    }

    public void updateAppConfig(String name, DatabaseType dbType, String jdbcUrl, String username, String appDsKey) {
        this.name = name;
        this.dbType = dbType;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.passwordEnc = null;
        this.authMode = AuthMode.PASSWORD;
        this.connectMode = ConnectMode.HOST_PORT;
        this.host = null;
        this.port = null;
        this.databaseName = null;
        this.serviceName = null;
        this.sid = null;
        this.driverClassName = null;
        this.extraParams = null;
        this.sourceType = DatabaseSourceSourceType.APP;
        this.appDsKey = appDsKey;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public boolean isAppSource() {
        return this.sourceType == DatabaseSourceSourceType.APP;
    }

    public void markTestOk(Instant now) {
        this.status = DatabaseSourceStatus.OK;
        this.lastTestTime = now;
        this.lastOkTime = now;
        this.lastError = null;
    }

    public void markTestFailed(Instant now, String error) {
        this.status = DatabaseSourceStatus.FAILED;
        this.lastTestTime = now;
        this.lastError = error;
    }

}

package com.corwin.datasource.interfaces.web;

import com.corwin.datasource.application.command.UpsertConnectionCommand;
import com.corwin.datasource.application.service.DatabaseSourceAdminService;
import com.corwin.datasource.application.view.DatabaseSourceSimpleView;
import com.corwin.datasource.application.view.TestConnectionView;
import com.corwin.datasource.domain.model.DatabaseColumn;
import com.corwin.datasource.domain.model.DatabaseSchema;
import com.corwin.datasource.domain.model.DatabaseSource;
import com.corwin.datasource.domain.model.DatabaseTable;
import com.corwin.datasource.interfaces.web.req.*;
import com.corwin.datasource.interfaces.web.res.*;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理接口。
 *
 * @author Corwin 2026/1/11
 */
@ApiMeta(module = ApiModuleCode.DATASOURCE)
@Authorize(userType = UserType.USER, permissions = {"ds.use"})
@RestController
@RequestMapping("/api/database-sources")
@AllArgsConstructor
public class DatabaseSourceAdminController {

    private final DatabaseSourceAdminService service;

    @PostMapping
    public ApiResponse<String> upsertDatabaseSource(@RequestBody UpsertConnectionReq req) {
        UpsertConnectionCommand cmd = new UpsertConnectionCommand(req.getId(), req.getName(), req.getDbType(),
                req.getAuthMode(), req.getConnectMode(), req.getHost(), req.getPort(), req.getDatabaseName(),
                req.getServiceName(), req.getSid(), req.getDriverClassName(), req.getJdbcUrl(), req.getUsername(),
                req.getPasswordRaw(), req.getDefaultSchema(), req.getRemarkCustom(), req.getExtraParams());
        return ApiResponse.ok(String.valueOf(service.upsertDatabaseSource(cmd)));
    }

    @PostMapping("/{id}/test")
    public ApiResponse<Object> test(@PathVariable("id") Long id) {
        service.testDatabaseSource(id);
        return ApiResponse.ok();
    }

    @PostMapping("/test-all")
    public ApiResponse<Object> testAll() {
        service.testAllDatabaseSources();
        return ApiResponse.ok();
    }

    @PostMapping("/test-config")
    public ApiResponse<TestConnectionRes> testConfig(@RequestBody UpsertConnectionReq req) {
        UpsertConnectionCommand cmd = new UpsertConnectionCommand(req.getId(), req.getName(), req.getDbType(),
                req.getAuthMode(), req.getConnectMode(), req.getHost(), req.getPort(), req.getDatabaseName(),
                req.getServiceName(), req.getSid(), req.getDriverClassName(), req.getJdbcUrl(), req.getUsername(),
                req.getPasswordRaw(), req.getDefaultSchema(), req.getRemarkCustom(), req.getExtraParams());
        TestConnectionView view = service.testDatabaseSourceConfig(cmd);
        return ApiResponse.ok(new TestConnectionRes(view.success(), view.message()));
    }

    @PostMapping("/page")
    public ApiResponse<PageResult<ConnectionRes>> pageDatabaseSources(@RequestBody ConnectionPageReq req) {
        var page = service.pageDatabaseSources(req.dbType(), req.nameLike(),
                PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, DatabaseSourceAdminController::toDto));
    }

    @GetMapping("/{id}")
    public ApiResponse<ConnectionRes> getDatabaseSource(@PathVariable Long id) {
        return ApiResponse.ok(toDto(service.getDatabaseSource(id)));
    }

    @GetMapping("/{id}/connection-info")
    public ApiResponse<DatabaseSourceConnectionInfoRes> getDatabaseSourceConnectionInfo(@PathVariable Long id) {
        var view = service.getDatabaseSourceConnectionInfo(id);
        return ApiResponse.ok(
                new DatabaseSourceConnectionInfoRes(view.id(), view.name(), view.dbType(), view.authMode(),
                        view.connectMode(), view.host(), view.port(), view.databaseName(), view.serviceName(),
                        view.sid(), view.driverClassName(), view.jdbcUrl(), view.username(), view.passwordRaw(),
                        view.remarkCustom(), view.extraParams()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteDatabaseSource(@PathVariable Long id) {
        service.deleteDatabaseSource(id);
        return ApiResponse.ok();
    }

    @GetMapping("/simple-list")
    public ApiResponse<List<DatabaseSourceSimpleRes>> listDatabaseSourcesSimple() {
        return ApiResponse.ok(
                service.listDatabaseSourcesSimple().stream().map(DatabaseSourceAdminController::toDto).toList());
    }

    @PostMapping("/database-schemas/list")
    public ApiResponse<List<CatalogRes>> listDatabaseSchemas(@RequestBody DatabaseSchemaListReq req) {
        DatabaseSchemaListReq resolved = req == null ? new DatabaseSchemaListReq(null, null, null, null) : req;
        boolean unboundOnly = Boolean.TRUE.equals(resolved.unboundOnly());
        String sortBy = resolved.sortBy() == null || resolved.sortBy().isBlank() ? "DATA_SOURCE" : resolved.sortBy();
        String sortDirection = resolved.sortDirection() == null || resolved.sortDirection()
                .isBlank() ? "ASC" : resolved.sortDirection();
        return ApiResponse.ok(
                service.listDatabaseSchemas(resolved.dataSourceId(), unboundOnly, sortBy, sortDirection).stream()
                        .map(DatabaseSourceAdminController::toDto).toList());
    }

    @GetMapping("/{id}/available-databases")
    public ApiResponse<List<String>> listAvailableDatabases(@PathVariable("id") Long id) {
        return ApiResponse.ok(service.listAvailableDatabases(id));
    }

    @PostMapping("/database-schemas")
    public ApiResponse<String> createDatabaseSchema(@RequestBody DatabaseSchemaCreateReq req) {
        return ApiResponse.ok(String.valueOf(
                service.createDatabaseSchema(req.getDataSourceId(), req.getDatabaseName(), req.getAlias(),
                        req.getRemarkCustom())));
    }

    @PostMapping("/database-schemas/{id}/refresh")
    public ApiResponse<Object> refreshMetadata(@PathVariable("id") Long id) {
        service.refreshDatabaseSchemaMetadata(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/database-schemas/{id}")
    public ApiResponse<Object> deleteDatabaseSchema(@PathVariable Long id) {
        service.deleteDatabaseSchema(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/tables/{id}")
    public ApiResponse<Object> deleteTable(@PathVariable Long id) {
        service.deleteTable(id);
        return ApiResponse.ok();
    }

    @GetMapping("/database-schemas/{id}")
    public ApiResponse<CatalogRes> getDatabaseSchema(@PathVariable Long id) {
        return ApiResponse.ok(toDto(service.getDatabaseSchema(id)));
    }

    @GetMapping("/database-schemas/{id}/basic-info")
    public ApiResponse<DatabaseSchemaBasicInfoRes> getDatabaseSchemaBasicInfo(@PathVariable("id") Long id) {
        var view = service.getDatabaseSchemaBasicInfo(id);
        return ApiResponse.ok(
                new DatabaseSchemaBasicInfoRes(view.dataSourceName(), view.dbType(), view.jdbcUrl(), view.username(),
                        view.passwordRaw()));
    }

    @PostMapping("/tables/page")
    public ApiResponse<PageResult<TableRes>> pageTables(@RequestBody TablePageReq req) {
        var page = service.pageTables(req.databaseId(), req.schema(), req.nameLike(), req.tableType(),
                PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, DatabaseSourceAdminController::toDto));
    }

    @PostMapping("/columns/page")
    public ApiResponse<PageResult<ColumnRes>> pageColumns(@RequestBody ColumnPageReq req) {
        var page = service.pageColumns(req.tableId(), req.nameLike(), PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, DatabaseSourceAdminController::toDto));
    }

    @GetMapping("/database-schemas/{id}/tables/all")
    public ApiResponse<List<TableRes>> listAllTables(@PathVariable("id") Long id) {
        return ApiResponse.ok(service.listAllTables(id).stream().map(DatabaseSourceAdminController::toDto).toList());
    }

    @GetMapping("/tables/{id}/columns/all")
    public ApiResponse<List<ColumnRes>> listAllColumns(@PathVariable("id") Long id) {
        return ApiResponse.ok(service.listAllColumns(id).stream().map(DatabaseSourceAdminController::toDto).toList());
    }

    @PutMapping("/tables/{id}/info")
    public ApiResponse<Object> updateTableInfo(@PathVariable("id") Long id, @RequestBody UpdateInfoReq req) {
        service.updateTableInfo(id, req.getAlias(), req.getRemarkCustom());
        return ApiResponse.ok();
    }

    @PutMapping("/columns/{id}/info")
    public ApiResponse<Object> updateColumnInfo(@PathVariable("id") Long id, @RequestBody UpdateInfoReq req) {
        service.updateColumnInfo(id, req.getAlias(), req.getRemarkCustom());
        return ApiResponse.ok();
    }

    @PutMapping("/database-schemas/{id}/source")
    public ApiResponse<Object> rebindDatabaseSchema(@PathVariable("id") Long id,
            @RequestBody DatabaseSchemaRebindReq req) {
        service.rebindDatabaseSchema(id, req.getDataSourceId());
        return ApiResponse.ok();
    }

    @PutMapping("/database-schemas/{id}/info")
    public ApiResponse<Object> updateDatabaseSchemaInfo(@PathVariable("id") Long id, @RequestBody UpdateInfoReq req) {
        service.updateDatabaseSchemaInfo(id, req.getAlias(), req.getRemarkCustom());
        return ApiResponse.ok();
    }

    // --- DTO Mappers ---

    private static ConnectionRes toDto(DatabaseSource c) {
        return new ConnectionRes(c.getId(), c.getName(), c.getDbType(), c.getAuthMode(), c.getConnectMode(),
                c.getHost(), c.getPort(), c.getDatabaseName(), c.getServiceName(), c.getSid(), c.getDriverClassName(),
                c.getJdbcUrl(), c.getUsername(), null, c.getRemarkCustom(), c.getExtraParams(), c.getSourceType(),
                c.getStatus().name(), c.getLastTestTime(), c.getLastOkTime(), c.getLastError());
    }

    private static CatalogRes toDto(DatabaseSchema c) {
        return new CatalogRes(c.getId(), c.getDataSourceId(), c.getDatabaseName(), c.getAlias(), c.getRemarkCustom(),
                c.getProductName(), c.getProductVersion(), c.getDriverName(), c.getDriverVersion(), c.getFetchedAt(),
                c.getCreatedAt());
    }

    private static DatabaseSourceSimpleRes toDto(DatabaseSourceSimpleView source) {
        return new DatabaseSourceSimpleRes(source.id(), source.name(), source.dbType(), source.username(),
                source.status(), source.sourceType());
    }

    private static TableRes toDto(DatabaseTable t) {
        return new TableRes(t.getId(), t.getDatabaseId(), t.getTableCatalog(), t.getTableSchema(), t.getTableName(),
                t.getTableType(), t.getRemarkDb(), t.getRemarkCustom(), t.getAlias());
    }

    private static ColumnRes toDto(DatabaseColumn c) {
        return new ColumnRes(c.getId(), c.getDatabaseId(), c.getTableId(), c.getColumnName(), c.getTypeName(),
                c.getJdbcType(), c.getColumnSize(), c.getDecimalDigits(), c.getNullable(), c.getOrdinalPosition(),
                c.getDefaultValue(), c.getRemarkDb(), c.getRemarkCustom(), c.getAlias());
    }

}

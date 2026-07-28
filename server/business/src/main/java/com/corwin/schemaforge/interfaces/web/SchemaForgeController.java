package com.corwin.schemaforge.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.schemaforge.application.command.CreateSchemaDdlCommand;
import com.corwin.schemaforge.application.command.CreateSchemaSnapshotCommand;
import com.corwin.schemaforge.application.command.UpdateSchemaDdlInfoCommand;
import com.corwin.schemaforge.application.command.UpdateSchemaSnapshotInfoCommand;
import com.corwin.schemaforge.application.service.SchemaDdlAppService;
import com.corwin.schemaforge.application.service.SchemaDiffAppService;
import com.corwin.schemaforge.application.service.SchemaSnapshotAppService;
import com.corwin.schemaforge.application.view.DiffResultView;
import com.corwin.schemaforge.application.view.SnapshotSelectableObjectView;
import com.corwin.schemaforge.interfaces.web.req.*;
import com.corwin.schemaforge.interfaces.web.res.DiffResultRes;
import com.corwin.schemaforge.interfaces.web.res.SchemaDdlRes;
import com.corwin.schemaforge.interfaces.web.res.SchemaSnapshotRes;
import com.corwin.schemaforge.interfaces.web.res.SnapshotSelectableObjectRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/2/24
 */
@ApiMeta(module = ApiModuleCode.SCHEMAFORGE)
@Authorize(userType = UserType.USER, permissions = {"sfg.use"})
@RestController
@RequestMapping("/api/sf")
@RequiredArgsConstructor
public class SchemaForgeController {

    private final SchemaSnapshotAppService schemaSnapshotAppService;
    private final SchemaDdlAppService schemaDdlAppService;
    private final SchemaDiffAppService schemaDiffAppService;

    @PostMapping("/snapshots")
    public ApiResponse<String> createSnapshot(@RequestBody CreateSchemaSnapshotReq req) {
        String id = schemaSnapshotAppService.createSnapshot(
                new CreateSchemaSnapshotCommand(req.managedDatabaseId(), req.name(), req.remark(),
                        req.selectedObjectIds()));
        return ApiResponse.ok(id);
    }

    @GetMapping("/snapshots/managed-databases/{managedDatabaseId}/objects")
    public ApiResponse<List<SnapshotSelectableObjectRes>> listSnapshotSelectableObjects(
            @PathVariable Long managedDatabaseId) {
        return ApiResponse.ok(schemaSnapshotAppService.listSnapshotSelectableObjects(managedDatabaseId).stream()
                .map(SchemaForgeController::toSnapshotSelectableObjectRes).toList());
    }

    @PostMapping("/snapshots/page")
    public ApiResponse<PageResult<SchemaSnapshotRes>> pageSnapshots(@RequestBody SchemaSnapshotPageReq req) {
        return ApiResponse.ok(schemaSnapshotAppService.pageQuery(req.managedDatabaseId(), req.nameLike(),
                PageSpecFactory.of(req.page(), req.sort())));
    }

    @GetMapping("/snapshots/options")
    public ApiResponse<List<SchemaSnapshotRes>> listSnapshotOptions() {
        return ApiResponse.ok(schemaSnapshotAppService.listAll());
    }

    @PutMapping("/snapshots/{id}")
    public ApiResponse<Object> updateSnapshot(@PathVariable String id, @RequestBody UpdateSchemaSnapshotReq req) {
        schemaSnapshotAppService.updateInfo(new UpdateSchemaSnapshotInfoCommand(id, req.name(), req.remark()));
        return ApiResponse.ok();
    }

    @DeleteMapping("/snapshots/{id}")
    public ApiResponse<Object> deleteSnapshot(@PathVariable String id) {
        schemaSnapshotAppService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/ddls")
    public ApiResponse<String> createDdl(@RequestBody CreateSchemaDdlReq req) {
        String id = schemaDdlAppService.createDdl(
                new CreateSchemaDdlCommand(req.sourceSnapshotId(), req.targetSnapshotId(), req.name(), req.remark()));
        return ApiResponse.ok(id);
    }

    @PostMapping("/ddls/page")
    public ApiResponse<PageResult<SchemaDdlRes>> pageDdls(@RequestBody SchemaDdlPageReq req) {
        return ApiResponse.ok(schemaDdlAppService.pageQuery(req.managedDatabaseId(), req.nameLike(),
                PageSpecFactory.of(req.page(), req.sort())));
    }

    @PutMapping("/ddls/{id}")
    public ApiResponse<Object> updateDdl(@PathVariable String id, @RequestBody UpdateSchemaDdlReq req) {
        schemaDdlAppService.updateInfo(new UpdateSchemaDdlInfoCommand(id, req.name(), req.remark()));
        return ApiResponse.ok();
    }

    @DeleteMapping("/ddls/{id}")
    public ApiResponse<Object> deleteDdl(@PathVariable String id) {
        schemaDdlAppService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/diff")
    public ApiResponse<DiffResultRes> diff(@ModelAttribute SchemaDiffQueryReq req) {
        DiffResultView view = schemaDiffAppService.compare(req.getRefDbId(), req.getTargetDbId());
        return ApiResponse.ok(
                new DiffResultRes(view.addedCount(), view.removedCount(), view.changedCount(), view.changeLogXml(),
                        view.changeSql()));
    }

    private static SnapshotSelectableObjectRes toSnapshotSelectableObjectRes(SnapshotSelectableObjectView view) {
        return new SnapshotSelectableObjectRes(view.id(), view.tableName(), view.tableSchema(), view.tableType(),
                view.alias());
    }
}

package com.corwin.jsonfmt.interfaces.web;

import com.corwin.framework.constant.UserType;
import com.corwin.framework.error.BaseError;
import com.corwin.framework.error.BizAssert;
import com.corwin.framework.web.auth.AuthPrincipal;
import com.corwin.framework.web.ctx.CtxUtil;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.jsonfmt.application.command.JsonFmtRecordBatchDeleteCommand;
import com.corwin.jsonfmt.application.command.JsonFmtRecordRenameCommand;
import com.corwin.jsonfmt.application.command.JsonFmtRecordReorderCommand;
import com.corwin.jsonfmt.application.command.JsonFmtRecordSaveCommand;
import com.corwin.jsonfmt.application.service.JsonFmtRecordAppService;
import com.corwin.jsonfmt.application.view.JsonFmtRecordDetailView;
import com.corwin.jsonfmt.application.view.JsonFmtRecordListItemView;
import com.corwin.jsonfmt.interfaces.web.req.JsonFmtRecordBatchDeleteReq;
import com.corwin.jsonfmt.interfaces.web.req.JsonFmtRecordListReq;
import com.corwin.jsonfmt.interfaces.web.req.JsonFmtRecordRenameReq;
import com.corwin.jsonfmt.interfaces.web.req.JsonFmtRecordReorderReq;
import com.corwin.jsonfmt.interfaces.web.req.JsonFmtRecordSaveReq;
import com.corwin.jsonfmt.interfaces.web.res.JsonFmtRecordDetailRes;
import com.corwin.jsonfmt.interfaces.web.res.JsonFmtRecordListItemRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/3/2
 */
@ApiMeta(module = ApiModuleCode.JSONFMT)
@Authorize(userType = UserType.USER, permissions = {"jfm.use"})
@RestController
@RequestMapping("/api/jsonfmt/records")
@RequiredArgsConstructor
public class JsonFmtRecordController {

    private final JsonFmtRecordAppService recordAppService;

    @PostMapping("/list")
    public ApiResponse<List<JsonFmtRecordListItemRes>> list(@RequestBody(required = false) JsonFmtRecordListReq req) {
        Long userId = currentUserId();
        String keyword = req == null ? null : req.keyword();
        List<JsonFmtRecordListItemRes> records = recordAppService.list(userId, keyword).stream()
                .map(JsonFmtRecordController::toListItemRes).toList();
        return ApiResponse.ok(records);
    }

    @GetMapping("/{recordId}")
    public ApiResponse<JsonFmtRecordDetailRes> detail(@PathVariable String recordId) {
        Long userId = currentUserId();
        JsonFmtRecordDetailView view = recordAppService.detail(userId, recordId);
        return ApiResponse.ok(toDetailRes(view));
    }

    @PostMapping
    public ApiResponse<JsonFmtRecordDetailRes> save(@RequestBody JsonFmtRecordSaveReq req) {
        Long userId = currentUserId();
        JsonFmtRecordDetailView view = recordAppService.save(userId,
                new JsonFmtRecordSaveCommand(req.id(), req.name(), req.content()));
        return ApiResponse.ok(toDetailRes(view));
    }

    @PutMapping("/{recordId}/name")
    public ApiResponse<Boolean> rename(@PathVariable String recordId, @RequestBody JsonFmtRecordRenameReq req) {
        Long userId = currentUserId();
        recordAppService.rename(userId, new JsonFmtRecordRenameCommand(recordId, req.name()));
        return ApiResponse.ok(true);
    }

    @PutMapping("/order")
    public ApiResponse<Boolean> reorder(@RequestBody JsonFmtRecordReorderReq req) {
        Long userId = currentUserId();
        recordAppService.reorder(userId, new JsonFmtRecordReorderCommand(req.orderedIds()));
        return ApiResponse.ok(true);
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Boolean> delete(@PathVariable String recordId) {
        Long userId = currentUserId();
        recordAppService.delete(userId, recordId);
        return ApiResponse.ok(true);
    }

    @PostMapping("/batch-delete")
    public ApiResponse<Boolean> batchDelete(@RequestBody JsonFmtRecordBatchDeleteReq req) {
        Long userId = currentUserId();
        recordAppService.batchDelete(userId, new JsonFmtRecordBatchDeleteCommand(req.recordIds()));
        return ApiResponse.ok(true);
    }

    private static JsonFmtRecordListItemRes toListItemRes(JsonFmtRecordListItemView view) {
        return new JsonFmtRecordListItemRes(view.id(), view.name(), view.orderNo(), view.updatedAt());
    }

    private static JsonFmtRecordDetailRes toDetailRes(JsonFmtRecordDetailView view) {
        return new JsonFmtRecordDetailRes(view.id(), view.name(), view.content(), view.orderNo(), view.createdAt(),
                view.updatedAt());
    }

    private Long currentUserId() {
        AuthPrincipal principal = CtxUtil.getPrincipal();
        Long userId = principal == null ? null : principal.userId();
        BizAssert.notNull(userId, BaseError.FORBIDDEN);
        return userId;
    }
}

package com.corwin.system.dict.interfaces.web;

import com.corwin.system.dict.application.service.DictQueryService;
import com.corwin.system.dict.application.view.DictItemView;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.dict.interfaces.web.req.DictBatchQueryReq;
import com.corwin.system.dict.interfaces.web.res.DictItemRes;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for querying dictionary data.
 * <p>Provides read-only endpoints to retrieve enabled dictionary items.</p>
 *
 * @author Corwin 2026/3/15
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/dicts")
@RequiredArgsConstructor
public class DictQueryController {

    private final DictQueryService dictQueryService;

    /**
     * Lists all enabled items of a dictionary type identified by its code.
     */
    @GetMapping("/{code}/items")
    @Authorize(userType = UserType.ADMIN, permissions = {"dict.view"})
    public ApiResponse<List<DictItemRes>> listItems(@PathVariable String code) {
        return ApiResponse.ok(dictQueryService.listEnabledItems(code).stream().map(this::toItemRes).toList());
    }

    /**
     * Batch-queries enabled items for multiple dictionary type codes.
     */
    @PostMapping("/batch-items")
    @Authorize(userType = UserType.ADMIN, permissions = {"dict.view"})
    public ApiResponse<Map<String, List<DictItemRes>>> batchItems(@RequestBody DictBatchQueryReq req) {
        Map<String, List<DictItemRes>> result = dictQueryService.batchListEnabledItems(req.codes()).entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(this::toItemRes).toList(), (left, right) -> left,
                        java.util.LinkedHashMap::new));
        return ApiResponse.ok(result);
    }

    /**
     * Resolves the display label for a given item value under the specified dict type.
     */
    @GetMapping("/{code}/label")
    @Authorize(userType = UserType.ADMIN, permissions = {"dict.view"})
    public ApiResponse<String> resolveLabel(@PathVariable String code, @RequestParam String value) {
        return ApiResponse.ok(dictQueryService.resolveLabel(code, value));
    }

    private DictItemRes toItemRes(DictItemView view) {
        return new DictItemRes(view.id(), view.dictTypeId(), view.parentItemId(), view.itemCode(), view.itemLabel(),
                view.itemValue(), view.sortNo(), view.enabled(), view.defaultItem(), view.tagColor(), view.tagType(),
                view.extraJson(), view.description());
    }
}

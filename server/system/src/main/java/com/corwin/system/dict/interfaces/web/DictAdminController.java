package com.corwin.system.dict.interfaces.web;

import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import com.corwin.system.dict.application.command.CreateDictItemCommand;
import com.corwin.system.dict.application.command.CreateDictTypeCommand;
import com.corwin.system.dict.application.command.SaveDictTypeItemCommand;
import com.corwin.system.dict.application.command.UpdateDictItemCommand;
import com.corwin.system.dict.application.command.UpdateDictTypeCommand;
import com.corwin.system.dict.application.service.DictAdminService;
import com.corwin.system.dict.application.view.DictItemView;
import com.corwin.system.dict.application.view.DictTypeView;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.dict.interfaces.web.req.CreateDictItemReq;
import com.corwin.system.dict.interfaces.web.req.CreateDictTypeReq;
import com.corwin.system.dict.interfaces.web.req.DictTypePageReq;
import com.corwin.system.dict.interfaces.web.req.SaveDictTypeItemReq;
import com.corwin.system.dict.interfaces.web.req.UpdateDictItemReq;
import com.corwin.system.dict.interfaces.web.req.UpdateDictItemSortReq;
import com.corwin.system.dict.interfaces.web.req.UpdateDictStatusReq;
import com.corwin.system.dict.interfaces.web.req.UpdateDictTypeReq;
import com.corwin.system.dict.interfaces.web.res.DictItemRes;
import com.corwin.system.dict.interfaces.web.res.DictTypeRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/3/15
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/sys/dicts/types")
@RequiredArgsConstructor
public class DictAdminController {

    private final DictAdminService dictAdminService;

    @PostMapping("/page")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.view"})
    public ApiResponse<PageResult<DictTypeRes>> list(@RequestBody DictTypePageReq req) {
        return ApiResponse.ok(
                PageResult.of(dictAdminService.pageTypes(req.keyword(), PageSpecFactory.of(req.page(), req.sort())),
                        this::toTypeRes));
    }

    @GetMapping("/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.view"})
    public ApiResponse<DictTypeRes> get(@PathVariable String id) {
        return ApiResponse.ok(toTypeRes(dictAdminService.getType(id)));
    }

    @PostMapping
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<DictTypeRes> create(@RequestBody CreateDictTypeReq req) {
        DictTypeView view = dictAdminService.createType(
                new CreateDictTypeCommand(req.code(), req.name(), req.description(), req.enumClass(), req.valueType(),
                        req.structureType(), req.enabled(), toItemCommands(req.items())));
        return ApiResponse.ok(toTypeRes(view));
    }

    @PutMapping("/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<DictTypeRes> update(@PathVariable String id, @RequestBody UpdateDictTypeReq req) {
        DictTypeView view = dictAdminService.updateType(id,
                new UpdateDictTypeCommand(req.name(), req.description(), req.enumClass(), req.valueType(),
                        req.structureType(), req.enabled(), toItemCommands(req.items())));
        return ApiResponse.ok(toTypeRes(view));
    }

    @PutMapping("/{id}/status")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<Boolean> updateStatus(@PathVariable String id, @RequestBody UpdateDictStatusReq req) {
        return ApiResponse.ok(dictAdminService.updateTypeStatus(id, req.enabled()));
    }

    @DeleteMapping("/{id}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<Boolean> delete(@PathVariable String id) {
        return ApiResponse.ok(dictAdminService.deleteType(id));
    }

    @GetMapping("/{id}/items")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.view"})
    public ApiResponse<List<DictItemRes>> listItems(@PathVariable String id) {
        return ApiResponse.ok(dictAdminService.listItems(id).stream().map(this::toItemRes).toList());
    }

    @PostMapping("/{id}/items")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<DictItemRes> createItem(@PathVariable String id, @RequestBody CreateDictItemReq req) {
        DictItemView view = dictAdminService.createItem(id,
                new CreateDictItemCommand(req.parentItemId(), req.itemCode(), req.itemLabel(), req.itemValue(),
                        req.sortNo(), req.enabled(), req.defaultItem(), req.tagColor(), req.tagType(), req.extraJson(),
                        req.description()));
        return ApiResponse.ok(toItemRes(view));
    }

    @PutMapping("/items/{itemId}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<DictItemRes> updateItem(@PathVariable String itemId, @RequestBody UpdateDictItemReq req) {
        DictItemView view = dictAdminService.updateItem(itemId,
                new UpdateDictItemCommand(req.parentItemId(), req.itemLabel(), req.itemValue(), req.sortNo(),
                        req.enabled(), req.defaultItem(), req.tagColor(), req.tagType(), req.extraJson(),
                        req.description()));
        return ApiResponse.ok(toItemRes(view));
    }

    @PutMapping("/items/{itemId}/status")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<Boolean> updateItemStatus(@PathVariable String itemId, @RequestBody UpdateDictStatusReq req) {
        return ApiResponse.ok(dictAdminService.updateItemStatus(itemId, req.enabled()));
    }

    @DeleteMapping("/items/{itemId}")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<Boolean> deleteItem(@PathVariable String itemId) {
        return ApiResponse.ok(dictAdminService.deleteItem(itemId));
    }

    @PutMapping("/{id}/items/sort")
    @Authorize(userTypes = {UserType.INTERNAL}, permissions = {"dict.edit"})
    public ApiResponse<Boolean> sortItems(@PathVariable String id, @RequestBody UpdateDictItemSortReq req) {
        return ApiResponse.ok(dictAdminService.sortItems(id, req.itemIds()));
    }

    private DictTypeRes toTypeRes(DictTypeView view) {
        return new DictTypeRes(view.id(), view.code(), view.name(), view.description(), view.enumClass(),
                view.valueType(), view.structureType(), view.sourceType(), view.enabled());
    }

    private DictItemRes toItemRes(DictItemView view) {
        return new DictItemRes(view.id(), view.dictTypeId(), view.parentItemId(), view.itemCode(), view.itemLabel(),
                view.itemValue(), view.sortNo(), view.enabled(), view.defaultItem(), view.tagColor(), view.tagType(),
                view.extraJson(), view.description());
    }

    private List<SaveDictTypeItemCommand> toItemCommands(List<SaveDictTypeItemReq> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(item -> new SaveDictTypeItemCommand(item.id(), item.clientKey(), item.parentClientKey(),
                        item.itemCode(), item.itemLabel(), item.itemValue(), item.sortNo(), item.enabled(),
                        item.defaultItem(), item.tagColor(), item.tagType(), item.extraJson(), item.description()))
                .toList();
    }

}

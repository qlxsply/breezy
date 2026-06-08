package com.corwin.clinic.interfaces.web;

import com.corwin.clinic.application.service.ClinicInventoryAppService;
import com.corwin.clinic.application.view.ItemInventoryView;
import com.corwin.clinic.interfaces.web.req.InventoryPageReq;
import com.corwin.clinic.interfaces.web.res.ItemInventoryRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Corwin 2026/2/9
 */
@ApiMeta(module = ApiModuleCode.CLINIC)
@Authorize(userTypes = UserType.EXTERNAL, permissions = {"cln.use"})
@RestController
@RequestMapping("/api/clinic/inventory")
@RequiredArgsConstructor
public class ClinicInventoryController {

    private final ClinicInventoryAppService inventoryAppService;

    @PostMapping("/page")
    public ApiResponse<PageResult<ItemInventoryRes>> page(@RequestBody InventoryPageReq req) {
        var page = inventoryAppService.page(req.skuId(), PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, ClinicInventoryController::toRes));
    }

    private static ItemInventoryRes toRes(ItemInventoryView view) {
        return new ItemInventoryRes(view.skuId(), view.skuDisplayName(), view.manufacturer(), view.spec(),
                view.inventoryQty());
    }
}

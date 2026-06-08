package com.corwin.clinic.interfaces.web;

import com.corwin.clinic.application.service.ClinicCatalogAppService;
import com.corwin.clinic.domain.model.ItemSku;
import com.corwin.clinic.domain.model.ItemSpu;
import com.corwin.clinic.domain.model.Supplier;
import com.corwin.clinic.interfaces.web.req.ItemSkuPageReq;
import com.corwin.clinic.interfaces.web.req.SupplierPageReq;
import com.corwin.clinic.interfaces.web.res.ItemSkuRes;
import com.corwin.clinic.interfaces.web.res.SupplierRes;
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

import java.util.Map;

/**
 * @author Corwin 2026/2/8
 */
@ApiMeta(module = ApiModuleCode.CLINIC)
@Authorize(userTypes = UserType.EXTERNAL, permissions = {"cln.use"})
@RestController
@RequestMapping("/api/clinic/catalog")
@RequiredArgsConstructor
public class ClinicCatalogController {

    private final ClinicCatalogAppService catalogAppService;

    @PostMapping("/skus/page")
    public ApiResponse<PageResult<ItemSkuRes>> pageSkus(@RequestBody ItemSkuPageReq req) {
        var page = catalogAppService.pageSkus(req.category(), req.nameLike(), req.enabled(),
                PageSpecFactory.of(req.page(), req.sort()));
        Map<Long, ItemSpu> spuMap = catalogAppService.mapSpus(
                page.elements().stream().map(ItemSku::getSpuId).distinct().toList());
        return ApiResponse.ok(PageResult.of(page, sku -> toSkuDto(sku, spuMap.get(sku.getSpuId()))));
    }

    @PostMapping("/suppliers/page")
    public ApiResponse<PageResult<SupplierRes>> pageSuppliers(@RequestBody SupplierPageReq req) {
        var page = catalogAppService.pageSuppliers(req.nameLike(), req.enabled(),
                PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, ClinicCatalogController::toSupplierDto));
    }


    private static ItemSkuRes toSkuDto(ItemSku sku, ItemSpu spu) {
        return new ItemSkuRes(sku.getId(), sku.getSpuId(), spu == null ? null : spu.getCategory(), sku.getDisplayName(),
                sku.getManufacturer(), sku.getSpec(), sku.isEnabled());
    }

    private static SupplierRes toSupplierDto(Supplier supplier) {
        return new SupplierRes(supplier.getId(), supplier.getName(), supplier.isEnabled(), supplier.getRemark());
    }
}

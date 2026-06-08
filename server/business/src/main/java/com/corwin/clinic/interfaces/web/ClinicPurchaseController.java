package com.corwin.clinic.interfaces.web;

import com.corwin.clinic.application.command.CreatePurchaseCommand;
import com.corwin.clinic.application.command.PurchaseLineCommand;
import com.corwin.clinic.application.service.ClinicPurchaseAppService;
import com.corwin.clinic.application.view.PurchaseDetailView;
import com.corwin.clinic.application.view.PurchaseLineView;
import com.corwin.clinic.application.view.PurchaseView;
import com.corwin.clinic.interfaces.web.req.PurchaseCreateReq;
import com.corwin.clinic.interfaces.web.req.PurchaseLineReq;
import com.corwin.clinic.interfaces.web.req.PurchasePageReq;
import com.corwin.clinic.interfaces.web.res.PurchaseDetailRes;
import com.corwin.clinic.interfaces.web.res.PurchaseLineRes;
import com.corwin.clinic.interfaces.web.res.PurchaseRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Corwin 2026/2/8
 */
@ApiMeta(module = ApiModuleCode.CLINIC)
@Authorize(userTypes = UserType.EXTERNAL, permissions = {"cln.use"})
@RestController
@RequestMapping("/api/clinic/purchases")
@RequiredArgsConstructor
public class ClinicPurchaseController {

    private final ClinicPurchaseAppService purchaseAppService;

    @PostMapping
    public ApiResponse<Long> create(@RequestBody PurchaseCreateReq req) {
        CreatePurchaseCommand cmd = new CreatePurchaseCommand(req.supplierId(), req.supplierName(), req.purchasedAt(),
                req.totalAmount(), req.remark(), toLineCommands(req.lines()));
        return ApiResponse.ok(purchaseAppService.create(cmd));
    }

    @PostMapping("/page")
    public ApiResponse<PageResult<PurchaseRes>> page(@RequestBody PurchasePageReq req) {
        var page = purchaseAppService.page(req.supplierId(), req.startAt(), req.endAt(),
                PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, ClinicPurchaseController::toDto));
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseDetailRes> detail(@PathVariable Long id) {
        PurchaseDetailView view = purchaseAppService.detail(id);
        return ApiResponse.ok(toDetailDto(view));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        purchaseAppService.delete(id);
        return ApiResponse.ok(null);
    }


    private static List<PurchaseLineCommand> toLineCommands(List<PurchaseLineReq> lines) {
        if (lines == null) {
            return null;
        }
        return lines.stream().map(ClinicPurchaseController::toCommand).toList();
    }

    private static PurchaseLineCommand toCommand(PurchaseLineReq line) {
        return new PurchaseLineCommand(line.skuId(), line.category(), line.itemName(), line.manufacturer(), line.spec(),
                line.qty(), line.unit(), line.lineTotalAmount(), line.remark());
    }

    private static PurchaseRes toDto(PurchaseView view) {
        return new PurchaseRes(view.id(), view.supplierId(), view.supplierName(), view.purchasedAt(),
                view.totalAmount(), view.remark());
    }

    private static PurchaseDetailRes toDetailDto(PurchaseDetailView view) {
        List<PurchaseLineRes> lines = view.lines() == null ? List.of() : view.lines().stream()
                .map(ClinicPurchaseController::toLineDto).toList();
        return new PurchaseDetailRes(view.id(), view.supplierId(), view.supplierName(), view.purchasedAt(),
                view.totalAmount(), view.remark(), lines);
    }

    private static PurchaseLineRes toLineDto(PurchaseLineView view) {
        return new PurchaseLineRes(view.id(), view.skuId(), view.skuDisplayName(), view.manufacturer(), view.spec(),
                view.qty(), view.unit(), view.lineTotalAmount(), view.unitPrice(), view.remark());
    }
}

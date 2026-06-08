package com.corwin.clinic.interfaces.web;

import com.corwin.clinic.application.command.CreateSaleCommand;
import com.corwin.clinic.application.command.SaleLineCommand;
import com.corwin.clinic.application.service.ClinicSaleAppService;
import com.corwin.clinic.application.view.SaleDetailView;
import com.corwin.clinic.application.view.SaleLineView;
import com.corwin.clinic.application.view.SaleView;
import com.corwin.clinic.interfaces.web.req.SaleCreateReq;
import com.corwin.clinic.interfaces.web.req.SaleLineReq;
import com.corwin.clinic.interfaces.web.req.SalePageReq;
import com.corwin.clinic.interfaces.web.res.SaleDetailRes;
import com.corwin.clinic.interfaces.web.res.SaleLineRes;
import com.corwin.clinic.interfaces.web.res.SaleRes;
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
@RequestMapping("/api/clinic/sales")
@RequiredArgsConstructor
public class ClinicSaleController {

    private final ClinicSaleAppService saleAppService;

    @PostMapping
    public ApiResponse<Long> create(@RequestBody SaleCreateReq req) {
        CreateSaleCommand cmd = new CreateSaleCommand(req.soldAt(), req.totalAmount(), req.customerName(), req.remark(),
                toLineCommands(req.lines()));
        return ApiResponse.ok(saleAppService.create(cmd));
    }

    @PostMapping("/page")
    public ApiResponse<PageResult<SaleRes>> page(@RequestBody SalePageReq req) {
        var page = saleAppService.page(req.customerNameLike(), req.startAt(), req.endAt(),
                PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, ClinicSaleController::toDto));
    }

    @GetMapping("/{id}")
    public ApiResponse<SaleDetailRes> detail(@PathVariable Long id) {
        SaleDetailView view = saleAppService.detail(id);
        return ApiResponse.ok(toDetailDto(view));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        saleAppService.delete(id);
        return ApiResponse.ok(null);
    }


    private static List<SaleLineCommand> toLineCommands(List<SaleLineReq> lines) {
        if (lines == null) {
            return null;
        }
        return lines.stream().map(ClinicSaleController::toCommand).toList();
    }

    private static SaleLineCommand toCommand(SaleLineReq line) {
        return new SaleLineCommand(line.skuId(), line.category(), line.itemName(), line.manufacturer(), line.spec(),
                line.qty(), line.unit(), line.lineTotalAmount(), line.remark());
    }

    private static SaleRes toDto(SaleView view) {
        return new SaleRes(view.id(), view.soldAt(), view.totalAmount(), view.customerName(), view.remark());
    }

    private static SaleDetailRes toDetailDto(SaleDetailView view) {
        List<SaleLineRes> lines = view.lines() == null ? List.of() : view.lines().stream()
                .map(ClinicSaleController::toLineDto).toList();
        return new SaleDetailRes(view.id(), view.soldAt(), view.totalAmount(), view.linesTotalAmount(),
                view.diffAmount(), view.customerName(), view.remark(), lines);
    }

    private static SaleLineRes toLineDto(SaleLineView view) {
        return new SaleLineRes(view.id(), view.skuId(), view.skuDisplayName(), view.manufacturer(), view.spec(),
                view.qty(), view.unit(), view.lineTotalAmount(), view.unitPrice(), view.remark());
    }
}

package com.corwin.clinic.interfaces.web;

import com.corwin.clinic.application.service.ClinicLedgerAppService;
import com.corwin.clinic.application.view.ItemLedgerView;
import com.corwin.clinic.interfaces.web.req.LedgerPageReq;
import com.corwin.clinic.interfaces.web.res.ItemLedgerRes;
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
 * @author Corwin 2026/2/8
 */
@ApiMeta(module = ApiModuleCode.CLINIC)
@Authorize(userTypes = UserType.EXTERNAL, permissions = {"cln.use"})
@RestController
@RequestMapping("/api/clinic/ledger")
@RequiredArgsConstructor
public class ClinicLedgerController {

    private final ClinicLedgerAppService ledgerAppService;

    @PostMapping("/page")
    public ApiResponse<PageResult<ItemLedgerRes>> page(@RequestBody LedgerPageReq req) {
        var page = ledgerAppService.page(req.skuId(), req.bizType(), req.supplierId(), req.startAt(), req.endAt(),
                PageSpecFactory.of(req.page(), req.sort()));
        return ApiResponse.ok(PageResult.of(page, ClinicLedgerController::toDto));
    }


    private static ItemLedgerRes toDto(ItemLedgerView view) {
        return new ItemLedgerRes(view.id(), view.skuId(), view.skuDisplayName(), view.bizType(), view.bizId(),
                view.bizLineId(), view.occurredAt(), view.qty(), view.unit(), view.amountTotal(), view.supplierId(),
                view.supplierName(), view.remark());
    }
}

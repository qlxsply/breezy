package com.corwin.system.methodstat.interfaces.web;

import com.corwin.system.methodstat.application.service.MethodStatQueryAppService;
import com.corwin.system.methodstat.application.view.MethodStatStatsView;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import com.corwin.system.methodstat.interfaces.web.req.MethodStatStatsPageReq;
import com.corwin.system.methodstat.interfaces.web.res.MethodStatStatsRes;
import com.corwin.framework.domain.page.SortSpec;
import com.corwin.system.auth.published.Authorize;
import com.corwin.framework.constant.UserType;
import com.corwin.framework.web.request.PageSpecFactory;
import com.corwin.framework.web.response.ApiResponse;
import com.corwin.framework.web.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller providing query endpoints for method statistics data.
 * @author Corwin 2026/3/25
 */
@ApiMeta(module = ApiModuleCode.METHODSTAT)
@RestController
@RequestMapping("/api/method-stat/query")
@RequiredArgsConstructor
public class MethodStatQueryController {

    private final MethodStatQueryAppService queryAppService;

    /**
     * Retrieve a paginated list of method statistics with optional filtering and sorting.
     * @param req the page request containing filter and sort parameters
     * @return paginated statistics response
     */
    @PostMapping("/stats/page")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.stat.view"})
    public ApiResponse<PageResult<MethodStatStatsRes>> pageStats(@RequestBody MethodStatStatsPageReq req) {
        SortSpec sortSpec = firstSort(req);
        var pageSpec = PageSpecFactory.of(req.page(), req.sort());
        var page = queryAppService.pageStats(req.methodName(), req.matchMode(),
                sortSpec == null ? null : sortSpec.field(),
                sortSpec == null || sortSpec.direction() == null ? null : sortSpec.direction().name(),
                pageSpec.pageNo(), pageSpec.pageSize());
        return ApiResponse.ok(PageResult.of(page, MethodStatQueryController::toStatsRes));
    }

    /**
     * Retrieve detailed statistics for a specific method by its key.
     * @param key the method key
     * @return the method stats detail
     */
    @GetMapping("/stats/detail")
    @Authorize(userType = UserType.ADMIN, permissions = {"mst.stat.view"})
    public ApiResponse<MethodStatStatsRes> methodStatsDetail(@RequestParam("key") String key) {
        return ApiResponse.ok(toStatsRes(queryAppService.getMethodStats(key)));
    }

    private static MethodStatStatsRes toStatsRes(MethodStatStatsView view) {
        return new MethodStatStatsRes(view.key(), view.packageName(), view.className(), view.methodName(),
                view.methodSignature(), view.methodSwitchEnabled(), view.globalSwitchEnabled(), view.collectEnabled(),
                view.totalCalls(), view.totalSuccess(), view.totalFailure(), view.recent1MinuteCalls(),
                view.recent1HourCalls(), view.recent1DayCalls(), view.recent1MinuteSuccess(),
                view.recent1MinuteFailure(), view.recent1HourSuccess(), view.recent1HourFailure(),
                view.recent1DaySuccess(), view.recent1DayFailure(), view.durationSampleSize(), view.durationMax(),
                view.durationMin(), view.durationAvg(), view.durationP50(), view.durationP90(), view.durationP95(),
                view.durationP99());
    }

    private static SortSpec firstSort(MethodStatStatsPageReq req) {
        if (req == null || req.sort() == null || req.sort().orders() == null) {
            return null;
        }
        List<SortSpec> orders = req.sort().orders();
        for (SortSpec sortSpec : orders) {
            if (sortSpec == null || sortSpec.field() == null || sortSpec.field().isBlank()) {
                continue;
            }
            return sortSpec;
        }
        return null;
    }
}

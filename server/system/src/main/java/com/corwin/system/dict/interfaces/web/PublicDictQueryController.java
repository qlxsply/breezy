package com.corwin.system.dict.interfaces.web;

import com.corwin.framework.web.response.ApiResponse;
import com.corwin.system.auth.published.PermitAll;
import com.corwin.system.dict.application.service.DictQueryService;
import com.corwin.system.dict.application.view.DictItemView;
import com.corwin.system.dict.interfaces.web.res.PublicDictItemRes;
import com.corwin.system.resource.published.ApiMeta;
import com.corwin.system.resource.published.ApiModuleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 面向业务页面的公共字典查询接口。
 *
 * @author Corwin 2026/8/5
 */
@ApiMeta(module = ApiModuleCode.SYSTEM)
@RestController
@RequestMapping("/api/public/dicts")
@RequiredArgsConstructor
public class PublicDictQueryController {

    private final DictQueryService dictQueryService;

    @GetMapping("/{code}/items")
    @PermitAll
    public ApiResponse<List<PublicDictItemRes>> listItems(@PathVariable String code) {
        return ApiResponse.ok(dictQueryService.listEnabledItems(code).stream().map(this::toRes).toList());
    }

    private PublicDictItemRes toRes(DictItemView view) {
        return new PublicDictItemRes(view.itemCode(), view.itemLabel(), view.itemValue(), view.tagColor(), view.tagType());
    }
}

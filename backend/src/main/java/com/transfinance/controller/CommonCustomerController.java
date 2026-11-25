package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.exception.BusinessException;
import com.transfinance.service.CommonCustomerService;
import com.transfinance.support.IndustryDataCatalog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** 银行身份取自服务端会话，浏览器不能通过机构参数切换被查询节点。 */
@RestController
@RequestMapping("/api/common-customers")
public class CommonCustomerController {
    private final CommonCustomerService service;
    public CommonCustomerController(CommonCustomerService service) { this.service = service; }
    public record Scope(@NotBlank String industryKey, @NotBlank String dataset) {}
    private String institution(HttpServletRequest request) {
        var session = request.getSession(false);
        Object bank = session == null ? null : session.getAttribute("financialInstitutionId");
        if (!(bank instanceof String value) || value.isBlank())
            throw new BusinessException(401, "请重新登录金融机构账号以建立隐私求交会话");
        return value;
    }
    @PostMapping
    public ApiResponse<CommonCustomerService.Result> find(@Valid @RequestBody Scope scope, HttpServletRequest request) {
        String bank = institution(request);
        var result = service.find(bank, scope.industryKey(), scope.dataset());
        // 仅保存本次核验的本方企业编码，标记接口不接受任意候选企业探测。
        request.getSession().setAttribute("psiMarkCandidates:" + bank, result.records().stream()
                .filter(row -> "DIFFERENT".equals(row.status())).map(row -> row.companyCode())
                .collect(java.util.stream.Collectors.toSet()));
        return ApiResponse.success(result);
    }
    @GetMapping("/status")
    public ApiResponse<List<CommonCustomerService.NodeStatus>> status(HttpServletRequest request) {
        institution(request);
        return ApiResponse.success(service.status());
    }
    @GetMapping("/industries")
    public ApiResponse<List<Map<String, String>>> industries(HttpServletRequest request) {
        institution(request);
        return ApiResponse.success(IndustryDataCatalog.industries().values().stream()
                .map(i -> Map.of("key", i.key(), "name", i.name())).toList());
    }
}

package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.exception.BusinessException;
import com.transfinance.service.CompanyReviewMarkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;

/** 标记只能针对当前会话真实求交返回的差异企业，机构身份不能由前端指定。 */
@RestController
@RequestMapping("/api/company-review-marks")
public class CompanyReviewMarkController {
    private final CompanyReviewMarkService service;
    public CompanyReviewMarkController(CompanyReviewMarkService service) { this.service = service; }
    public record MarkRequest(@NotBlank @Size(max = 100) String companyCode) {}
    private String bank(HttpServletRequest request) {
        var session = request.getSession(false);
        var value = session == null ? null : session.getAttribute("financialInstitutionId");
        if (!(value instanceof String bank) || bank.isBlank()) throw new BusinessException(401, "请重新登录金融机构账号");
        return bank;
    }
    @GetMapping
    public ApiResponse<List<String>> list(HttpServletRequest request) {
        return ApiResponse.success(service.findCodes(bank(request)));
    }
    @PostMapping
    public ApiResponse<Boolean> mark(@Valid @RequestBody MarkRequest body, HttpServletRequest request) {
        String bank = bank(request);
        Object candidates = request.getSession().getAttribute("psiMarkCandidates:" + bank);
        if (!(candidates instanceof Set<?> codes) || !codes.contains(body.companyCode()))
            throw new BusinessException("请先查找共同客户，再标记存在差异的企业");
        service.mark(bank, body.companyCode());
        return ApiResponse.success(true);
    }
}

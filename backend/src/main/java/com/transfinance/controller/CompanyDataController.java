package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.CompanyDataQueryRequest;
import com.transfinance.service.CompanyDataService;
import com.transfinance.vo.CompanyAssessmentDetailVO;
import com.transfinance.vo.CompanyAssessmentPageResponse;
import com.transfinance.vo.IndustryOptionsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业端历史数据与待评估数据接口。
 */
@RestController
@RequestMapping("/api/company-data")
public class CompanyDataController {

    private final CompanyDataService companyDataService;

    public CompanyDataController(CompanyDataService companyDataService) {
        this.companyDataService = companyDataService;
    }

    @GetMapping("/history")
    public ApiResponse<CompanyAssessmentPageResponse> history(@Valid CompanyDataQueryRequest request) {
        return ApiResponse.success(companyDataService.page(request, false));
    }

    @GetMapping("/pending")
    public ApiResponse<CompanyAssessmentPageResponse> pending(@Valid CompanyDataQueryRequest request) {
        return ApiResponse.success(companyDataService.page(request, true));
    }

    @GetMapping("/history/options")
    public ApiResponse<IndustryOptionsResponse> historyOptions(@RequestParam String companyCode,
                                                                @RequestParam(required = false) String industryKey) {
        return ApiResponse.success(companyDataService.options(companyCode, industryKey, false));
    }

    @GetMapping("/pending/options")
    public ApiResponse<IndustryOptionsResponse> pendingOptions(@RequestParam String companyCode,
                                                                @RequestParam(required = false) String industryKey) {
        return ApiResponse.success(companyDataService.options(companyCode, industryKey, true));
    }

    @GetMapping("/history/{industryKey}/{id}")
    public ApiResponse<CompanyAssessmentDetailVO> historyDetail(@PathVariable String industryKey,
                                                                 @PathVariable Long id,
                                                                 @RequestParam String companyCode) {
        return ApiResponse.success(companyDataService.detail(companyCode, industryKey, id, false));
    }

    @GetMapping("/pending/{industryKey}/{id}")
    public ApiResponse<CompanyAssessmentDetailVO> pendingDetail(@PathVariable String industryKey,
                                                                 @PathVariable Long id,
                                                                 @RequestParam String companyCode) {
        return ApiResponse.success(companyDataService.detail(companyCode, industryKey, id, true));
    }
}

package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.common.PageResult;
import com.transfinance.service.CompanyMissingDataService;
import com.transfinance.vo.FinancialDataChangeNoticeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 金融机构端查看企业补全结果通知接口。
 */
@RestController
@RequestMapping("/api/financial-data-notices")
public class FinancialDataNoticeController {

    private final CompanyMissingDataService companyMissingDataService;

    public FinancialDataNoticeController(CompanyMissingDataService companyMissingDataService) {
        this.companyMissingDataService = companyMissingDataService;
    }

    @GetMapping
    public ApiResponse<PageResult<FinancialDataChangeNoticeVO>> page(@RequestParam String institutionId,
                                                                     @RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) String industryKey,
                                                                     @RequestParam(required = false) Integer dataYear,
                                                                     @RequestParam(required = false) String status,
                                                                     @RequestParam(defaultValue = "1") Long page,
                                                                     @RequestParam(defaultValue = "10") Long size) {
        return ApiResponse.success(companyMissingDataService.financialNotices(
                institutionId,
                keyword,
                industryKey,
                dataYear,
                status,
                page,
                size
        ));
    }
}

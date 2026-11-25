package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.common.PageResult;
import com.transfinance.dto.CompanyMissingDataSaveRequest;
import com.transfinance.service.CompanyMissingDataService;
import com.transfinance.vo.CompanyMissingReminderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 企业端缺失数据提醒与补全接口。
 */
@RestController
@RequestMapping("/api/company-missing-data")
public class CompanyMissingDataController {

    private final CompanyMissingDataService companyMissingDataService;

    public CompanyMissingDataController(CompanyMissingDataService companyMissingDataService) {
        this.companyMissingDataService = companyMissingDataService;
    }

    @GetMapping
    public ApiResponse<PageResult<CompanyMissingReminderVO>> page(@RequestParam String companyCode,
                                                                  @RequestParam(defaultValue = "1") Long page,
                                                                  @RequestParam(defaultValue = "10") Long size) {
        return ApiResponse.success(companyMissingDataService.companyReminders(companyCode, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id,
                                                   @RequestParam String companyCode) {
        return ApiResponse.success(companyMissingDataService.reminderDetail(companyCode, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> save(@PathVariable Long id,
                                  @Valid @RequestBody CompanyMissingDataSaveRequest request) {
        companyMissingDataService.saveMissingValues(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable Long id,
                                    @RequestParam String companyCode) {
        companyMissingDataService.submitReminder(companyCode, id);
        return ApiResponse.success();
    }
}

package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.ElectricCompanyDataQueryRequest;
import com.transfinance.dto.IndustryCompanyDataSaveRequest;
import com.transfinance.dto.PendingScoreRequest;
import com.transfinance.service.ElectricCompanyDataService;
import com.transfinance.vo.IndustryCompanyDataPageResponse;
import com.transfinance.vo.IndustryOptionsResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 待评估企业数据接口。
 */
@RestController
@RequestMapping("/api/pending-company-data")
public class PendingCompanyDataController {

    private final ElectricCompanyDataService dataService;

    public PendingCompanyDataController(ElectricCompanyDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping
    public ApiResponse<IndustryCompanyDataPageResponse> page(@Valid ElectricCompanyDataQueryRequest request) {
        return ApiResponse.success(dataService.pendingPage(request));
    }

    @GetMapping("/options")
    public ApiResponse<IndustryOptionsResponse> options(@RequestParam String institutionId,
                                                        @RequestParam(required = false) String industryKey) {
        return ApiResponse.success(dataService.pendingOptions(institutionId, industryKey));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody IndustryCompanyDataSaveRequest request) {
        dataService.createPending(request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody IndustryCompanyDataSaveRequest request) {
        dataService.updatePending(id, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/credit")
    public ApiResponse<Void> credit(@PathVariable Long id, @Valid @RequestBody PendingScoreRequest request) {
        dataService.creditPending(id, request.getInstitutionId(), request.getIndustryKey());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/score")
    public ApiResponse<Void> score(@PathVariable Long id, @Valid @RequestBody PendingScoreRequest request) {
        dataService.scorePending(id, request.getInstitutionId(), request.getIndustryKey(), request.getModelMode());
        return ApiResponse.success();
    }

    @DeleteMapping("/{industryKey}/{institutionId}/{id}")
    public ApiResponse<Void> deleteById(@PathVariable String industryKey,
                                        @PathVariable String institutionId,
                                        @PathVariable Long id) {
        dataService.deletePendingById(industryKey, institutionId, id);
        return ApiResponse.success();
    }
}

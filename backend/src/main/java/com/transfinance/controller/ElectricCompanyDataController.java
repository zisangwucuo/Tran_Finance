package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.ElectricCompanyDataQueryRequest;
import com.transfinance.dto.IndustryCompanyDataSaveRequest;
import com.transfinance.service.ElectricCompanyDataService;
import com.transfinance.vo.IndustryCompanyDataPageResponse;
import com.transfinance.vo.IndustryOptionsResponse;
import com.transfinance.vo.TransitionScoreTrendVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 企业评估数据接口。
 */
@RestController
@RequestMapping("/api/electric-company-data")
public class ElectricCompanyDataController {

    private final ElectricCompanyDataService dataService;

    public ElectricCompanyDataController(ElectricCompanyDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping
    public ApiResponse<IndustryCompanyDataPageResponse> page(@Valid ElectricCompanyDataQueryRequest request) {
        return ApiResponse.success(dataService.page(request));
    }

    @GetMapping("/options")
    public ApiResponse<IndustryOptionsResponse> options(@RequestParam String institutionId,
                                                        @RequestParam(required = false) String industryKey) {
        return ApiResponse.success(dataService.options(institutionId, industryKey));
    }

    @GetMapping("/transition-score-trend")
    public ApiResponse<List<TransitionScoreTrendVO>> transitionScoreTrend(@RequestParam String institutionId,
                                                                          @RequestParam String industryKey,
                                                                          @RequestParam String companyCode) {
        return ApiResponse.success(dataService.transitionScoreTrend(institutionId, industryKey, companyCode));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody IndustryCompanyDataSaveRequest request) {
        dataService.create(request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody IndustryCompanyDataSaveRequest request) {
        dataService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{industryKey}/{institutionId}/{id}")
    public ApiResponse<Void> deleteById(@PathVariable String industryKey,
                                        @PathVariable String institutionId,
                                        @PathVariable Long id) {
        dataService.deleteById(industryKey, institutionId, id);
        return ApiResponse.success();
    }
}

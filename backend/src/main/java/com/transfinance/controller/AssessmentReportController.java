package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.service.AssessmentReportService;
import com.transfinance.vo.AssessmentReportVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 转型金融评估报告接口。
 */
@RestController
@RequestMapping("/api/assessment-reports")
public class AssessmentReportController {

    private final AssessmentReportService reportService;
    private final boolean aiEnabled;
    private final String aiApiUrl;
    private final String aiApiKey;
    private final String aiModel;
    private final boolean responseFormatJson;
    private final boolean deepseekThinkingDisabled;

    public AssessmentReportController(AssessmentReportService reportService,
                                      @Value("${ai.narrative.enabled:false}") boolean aiEnabled,
                                      @Value("${ai.narrative.api-url:}") String aiApiUrl,
                                      @Value("${ai.narrative.api-key:}") String aiApiKey,
                                      @Value("${ai.narrative.model:}") String aiModel,
                                      @Value("${ai.narrative.response-format-json:false}") boolean responseFormatJson,
                                      @Value("${ai.narrative.deepseek-thinking-disabled:false}") boolean deepseekThinkingDisabled) {
        this.reportService = reportService;
        this.aiEnabled = aiEnabled;
        this.aiApiUrl = aiApiUrl;
        this.aiApiKey = aiApiKey;
        this.aiModel = aiModel;
        this.responseFormatJson = responseFormatJson;
        this.deepseekThinkingDisabled = deepseekThinkingDisabled;
    }

    @GetMapping("/pending/{industryKey}/{institutionId}/{id}")
    public ApiResponse<AssessmentReportVO> pendingReport(@PathVariable String industryKey,
                                                         @PathVariable String institutionId,
                                                         @PathVariable Long id,
                                                         @RequestParam(defaultValue = "local") String modelMode) {
        return ApiResponse.success(reportService.buildPendingReport(institutionId, industryKey, id, modelMode));
    }

    @GetMapping("/ai-status")
    public ApiResponse<Map<String, Object>> aiStatus() {
        return ApiResponse.success(Map.of(
                "enabled", aiEnabled,
                "apiUrlConfigured", StringUtils.hasText(aiApiUrl),
                "apiKeyConfigured", StringUtils.hasText(aiApiKey),
                "modelConfigured", StringUtils.hasText(aiModel),
                "model", StringUtils.hasText(aiModel) ? aiModel : "",
                "responseFormatJson", responseFormatJson,
                "deepseekThinkingDisabled", deepseekThinkingDisabled
        ));
    }
}

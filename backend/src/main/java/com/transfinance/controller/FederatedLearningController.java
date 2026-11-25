package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.ModelTrainingRequest;
import com.transfinance.service.FederatedLearningService;
import com.transfinance.vo.FederatedLearningSummaryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 横向联邦学习接口。
 */
@RestController
@RequestMapping("/api/federated-learning")
public class FederatedLearningController {

    private final FederatedLearningService federatedLearningService;

    public FederatedLearningController(FederatedLearningService federatedLearningService) {
        this.federatedLearningService = federatedLearningService;
    }

    @GetMapping("/summary")
    public ApiResponse<FederatedLearningSummaryVO> summary() {
        return ApiResponse.success(federatedLearningService.summary());
    }

    @PostMapping("/train")
    public ApiResponse<Object> train(@Valid @RequestBody ModelTrainingRequest request) {
        return ApiResponse.success(federatedLearningService.train(request.getInstitutionId()));
    }

    @PostMapping("/clear")
    public ApiResponse<Object> clear() {
        return ApiResponse.success(federatedLearningService.clear());
    }
}

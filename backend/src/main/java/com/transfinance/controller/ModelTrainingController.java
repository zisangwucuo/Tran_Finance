package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.ModelTrainingRequest;
import com.transfinance.service.ModelTrainingService;
import com.transfinance.vo.ModelTrainingSummaryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型训练与评估设置接口。
 */
@RestController
@RequestMapping("/api/model-training")
public class ModelTrainingController {

    private final ModelTrainingService modelTrainingService;

    public ModelTrainingController(ModelTrainingService modelTrainingService) {
        this.modelTrainingService = modelTrainingService;
    }

    @GetMapping("/summary")
    public ApiResponse<ModelTrainingSummaryVO> summary(@RequestParam String institutionId) {
        return ApiResponse.success(modelTrainingService.summary(institutionId));
    }

    @PostMapping("/train-all")
    public ApiResponse<Object> trainAll(@Valid @RequestBody ModelTrainingRequest request) {
        return ApiResponse.success(modelTrainingService.trainAll(request.getInstitutionId(), Boolean.TRUE.equals(request.getForce())));
    }

    @PostMapping("/update")
    public ApiResponse<Object> updateModels(@Valid @RequestBody ModelTrainingRequest request) {
        return ApiResponse.success(modelTrainingService.updateModels(request.getInstitutionId(), Boolean.TRUE.equals(request.getForce()), request.getParameters()));
    }

    @PostMapping("/clear")
    public ApiResponse<Object> clearModels(@Valid @RequestBody ModelTrainingRequest request) {
        return ApiResponse.success(modelTrainingService.clearModels(request.getInstitutionId()));
    }

    @PostMapping("/notify-companies")
    public ApiResponse<Object> notifyCompanies(@Valid @RequestBody ModelTrainingRequest request) {
        return ApiResponse.success(modelTrainingService.notifyCompanies(request.getInstitutionId()));
    }
}

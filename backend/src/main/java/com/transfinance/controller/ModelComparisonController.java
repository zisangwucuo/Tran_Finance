package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.ModelComparisonRequest;
import com.transfinance.exception.BusinessException;
import com.transfinance.service.ModelComparisonService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 登录机构只能执行、读取自己的模型对比，不信任前端传入的机构编码。 */
@RestController
@RequestMapping("/api/model-comparisons")
public class ModelComparisonController {
    private final ModelComparisonService service;
    public ModelComparisonController(ModelComparisonService service) { this.service=service; }
    private String bank(HttpServletRequest request) {
        var session=request.getSession(false);
        Object bank=session==null?null:session.getAttribute("financialInstitutionId");
        if(bank==null) throw new BusinessException(401,"请重新登录金融机构账户");
        return bank.toString();
    }
    @GetMapping public ApiResponse<Object> list(HttpServletRequest request) {
        return ApiResponse.success(service.list(bank(request)));
    }
    @PostMapping public ApiResponse<Object> start(@Valid @RequestBody ModelComparisonRequest body,HttpServletRequest request) {
        return ApiResponse.success(service.start(bank(request),body.industryKey(),body.effectiveMode()));
    }
}

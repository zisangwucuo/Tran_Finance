package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.CustomerOverviewQueryRequest;
import com.transfinance.exception.BusinessException;
import com.transfinance.service.CustomerOverviewService;
import com.transfinance.vo.CustomerOverviewVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 客户总览只读接口：仅使用服务端会话中的银行身份，不信任查询参数或本地存储。 */
@RestController
@RequestMapping("/api/customer-overview")
public class CustomerOverviewController {
    private final CustomerOverviewService service;

    public CustomerOverviewController(CustomerOverviewService service) { this.service = service; }

    @GetMapping
    public ApiResponse<CustomerOverviewVO> overview(@Valid CustomerOverviewQueryRequest query, HttpServletRequest request) {
        var session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("financialInstitutionId");
        if (!(value instanceof String bank) || bank.isBlank()) throw new BusinessException(401, "请重新登录金融机构账号");
        return ApiResponse.success(service.overview(bank, query.dataYear()));
    }
}

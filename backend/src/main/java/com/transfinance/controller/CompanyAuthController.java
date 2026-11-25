package com.transfinance.controller;

import com.transfinance.common.ApiResponse;
import com.transfinance.dto.CompanyBootstrapRequest;
import com.transfinance.dto.CompanyProfileUpdateRequest;
import com.transfinance.dto.CompanyRegisterRequest;
import com.transfinance.dto.LoginRequest;
import com.transfinance.service.CompanyAuthService;
import com.transfinance.service.CompanyBootstrapService;
import com.transfinance.vo.CompanyLoginResponse;
import com.transfinance.vo.CompanyProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

/**
 * 企业端认证与资料接口。
 */
@RestController
@RequestMapping("/api/company-auth")
public class CompanyAuthController {

    private final CompanyAuthService companyAuthService;
    private final CompanyBootstrapService companyBootstrapService;

    public CompanyAuthController(CompanyAuthService companyAuthService,
                                 CompanyBootstrapService companyBootstrapService) {
        this.companyAuthService = companyAuthService;
        this.companyBootstrapService = companyBootstrapService;
    }

    @PostMapping("/login")
    public ApiResponse<CompanyLoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                   jakarta.servlet.http.HttpServletRequest http) {
        CompanyLoginResponse user = companyAuthService.login(request);
        if (http.getSession(false) != null) http.changeSessionId();
        http.getSession().setAttribute("companyCode", user.getCompanyCode());
        return ApiResponse.success(user);
    }

    @PostMapping("/register")
    public ApiResponse<CompanyLoginResponse> register(@Valid @RequestBody CompanyRegisterRequest request,
                                                      jakarta.servlet.http.HttpServletRequest http) {
        CompanyLoginResponse user = companyAuthService.register(request);
        if (http.getSession(false) != null) http.changeSessionId();
        http.getSession().setAttribute("companyCode", user.getCompanyCode());
        return ApiResponse.success(user);
    }

    /** 新增材料接口依赖服务端会话；退出时清理企业身份。 */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(jakarta.servlet.http.HttpServletRequest http) {
        if (http.getSession(false) != null) http.getSession(false).removeAttribute("companyCode");
        return ApiResponse.success();
    }

    @GetMapping("/profile")
    public ApiResponse<CompanyProfileVO> profile(@RequestParam String companyCode) {
        return ApiResponse.success(companyAuthService.profile(companyCode));
    }

    @PutMapping("/profile")
    public ApiResponse<CompanyProfileVO> updateProfile(@Valid @RequestBody CompanyProfileUpdateRequest request) {
        return ApiResponse.success(companyAuthService.updateProfile(request));
    }

    /**
     * 从历史数据和待评估数据中补齐企业基础表与默认企业账号。
     */
    @PostMapping("/bootstrap-from-data")
    public ApiResponse<Void> bootstrapFromData(@RequestBody(required = false) CompanyBootstrapRequest request) {
        String defaultPassword = request == null ? null : request.getDefaultPassword();
        if (StringUtils.hasText(defaultPassword)) {
            companyBootstrapService.bootstrapCompaniesFromHistory(defaultPassword);
        } else {
            companyBootstrapService.bootstrapCompaniesFromHistory();
        }
        return ApiResponse.success();
    }
}
